package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.utils.StringUtils;
import com.cqwu.cqwu_appointment_system.domain.dto.FrontendLogRequestDTO;
import com.cqwu.cqwu_appointment_system.domain.query.FrontendLogQuery;
import com.cqwu.cqwu_appointment_system.entity.FrontendLogExample;
import com.cqwu.cqwu_appointment_system.entity.FrontendLogWithBLOBs;
import com.cqwu.cqwu_appointment_system.mapper.FrontendLogMapper;
import com.cqwu.cqwu_appointment_system.service.LogService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description LogService 的实现类。
 */
@Service
@Slf4j
public class LogServiceImpl implements LogService {

    @Value("${logging.file.path:./logs}")
    private String logPath;

    @Value("${logging.frontend.max-per-user:20}")
    private int maxLogsPerUser;

    @Autowired
    private FrontendLogMapper frontendLogMapper;

    private static final List<String> VALID_LOG_LEVELS = List.of("debug", "info", "warn", "error");
    private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Pattern LOG_TIMESTAMP_PATTERN = Pattern
            .compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})");

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getBackendLogs(String level, int lines, LocalDateTime beforeTimestamp) throws IOException {
        String validatedLevel = level.toLowerCase();
        if (!VALID_LOG_LEVELS.contains(validatedLevel)) {
            throw new IllegalArgumentException("无效的日志级别: " + level);
        }

        String fileName = String.format("sys-%s.log", validatedLevel);
        File logFile = new File(logPath, fileName);

        if (!logFile.exists() || !logFile.canRead()) {
            return Collections.singletonList("日志文件不存在或不可读: " + logFile.getAbsolutePath());
        }

        if (beforeTimestamp == null) {
            return readLastNLines(logFile, lines);
        } else {
            return readNLinesBefore(logFile, lines, beforeTimestamp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void saveFrontendLog(FrontendLogRequestDTO dto, String userId, String userAgent) {
        FrontendLogExample countExample = new FrontendLogExample();
        countExample.createCriteria().andUserIdEqualTo(userId);
        long currentCount = frontendLogMapper.countByExample(countExample);

        if (currentCount >= maxLogsPerUser) {
            FrontendLogExample selectOldestExample = new FrontendLogExample();
            selectOldestExample.createCriteria().andUserIdEqualTo(userId);
            selectOldestExample.setOrderByClause("log_timestamp ASC, id ASC");

            PageHelper.startPage(1, 1);
            List<FrontendLogWithBLOBs> oldestLogs = frontendLogMapper.selectByExampleWithBLOBs(selectOldestExample);

            if (!oldestLogs.isEmpty()) {
                frontendLogMapper.deleteByPrimaryKey(oldestLogs.get(0).getId());
            }
        }

        FrontendLogWithBLOBs log = new FrontendLogWithBLOBs();
        log.setLogLevel(dto.getLogLevel().toUpperCase());
        log.setLogTimestamp(dto.getLogTimestamp());
        log.setLogMessage(dto.getLogMessage());

        if (StringUtils.hasText(userId)) {
            log.setUserId(userId);
        }
        if (StringUtils.hasText(userAgent)) {
            log.setUserAgent(userAgent);
        }

        frontendLogMapper.insertSelective(log);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void saveFrontendLogs(List<FrontendLogRequestDTO> list, String userId, String userAgent) {
        if (list == null || list.isEmpty()) {
            return;
        }
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        List<FrontendLogRequestDTO> toInsert = new ArrayList<>(list);
        FrontendLogExample countExample = new FrontendLogExample();
        countExample.createCriteria().andUserIdEqualTo(userId);
        long currentCount = frontendLogMapper.countByExample(countExample);

        long overflow = currentCount + toInsert.size() - maxLogsPerUser;
        if (overflow > 0) {
            if (overflow <= currentCount) {
                // 删除历史中最旧的 overflow 条
                FrontendLogExample selectOldest = new FrontendLogExample();
                selectOldest.createCriteria().andUserIdEqualTo(userId);
                selectOldest.setOrderByClause("log_timestamp ASC, id ASC");
                com.github.pagehelper.PageHelper.startPage(1, (int) overflow);
                List<FrontendLogWithBLOBs> olds = frontendLogMapper.selectByExampleWithBLOBs(selectOldest);
                for (FrontendLogWithBLOBs old : olds) {
                    frontendLogMapper.deleteByPrimaryKey(old.getId());
                }
            } else {
                // 现有全删仍不足以腾出空间，说明本次批量本身超过上限
                // 1) 删除该用户所有旧日志
                FrontendLogExample delAll = new FrontendLogExample();
                delAll.createCriteria().andUserIdEqualTo(userId);
                frontendLogMapper.deleteByExample(delAll);
                // 2) 仅保留本次批量中最新的 maxLogsPerUser 条
                toInsert.sort(Comparator.comparing(FrontendLogRequestDTO::getLogTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));
                int keep = Math.min(maxLogsPerUser, toInsert.size());
                toInsert = new ArrayList<>(toInsert.subList(toInsert.size() - keep, toInsert.size()));
            }
        }

        // 批量插入：按时间顺序构建实体并调用 batchInsert
        toInsert.sort(Comparator.comparing(FrontendLogRequestDTO::getLogTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));
        List<FrontendLogWithBLOBs> rows = new ArrayList<>(toInsert.size());
        for (FrontendLogRequestDTO dto : toInsert) {
            FrontendLogWithBLOBs log = new FrontendLogWithBLOBs();
            if (StringUtils.hasText(dto.getLogLevel())) {
                log.setLogLevel(dto.getLogLevel().toUpperCase());
            }
            log.setLogTimestamp(dto.getLogTimestamp());
            log.setLogMessage(dto.getLogMessage());
            if (StringUtils.hasText(userId)) {
                log.setUserId(userId);
            }
            if (StringUtils.hasText(userAgent)) {
                log.setUserAgent(userAgent);
            }
            rows.add(log);
        }
        if (!rows.isEmpty()) {
            frontendLogMapper.batchInsert(rows);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<FrontendLogWithBLOBs> queryFrontendLogs(FrontendLogQuery query) {
        log.info("开始根据条件分页查询前端日志, 查询参数: {}", query);

        Integer page = query.getPage() == null ? Constants.DEFAULT_PAGE : query.getPage();
        Integer size = query.getSize() == null ? Constants.DEFAULT_PAGE_SIZE : query.getSize();

        PageHelper.startPage(page + 1, size);

        FrontendLogExample example = new FrontendLogExample();
        FrontendLogExample.Criteria criteria = example.createCriteria();

        if (StringUtils.hasText(query.getLogLevel())) {
            criteria.andLogLevelEqualTo(query.getLogLevel());
        }
        if (StringUtils.hasText(query.getUserId())) {
            criteria.andUserIdEqualTo(query.getUserId());
        }
        if (query.getStartTime() != null) {
            criteria.andLogTimestampGreaterThanOrEqualTo(query.getStartTime());
        }
        if (query.getEndTime() != null) {
            criteria.andLogTimestampLessThanOrEqualTo(query.getEndTime());
        }

        example.setOrderByClause("log_timestamp DESC");

        List<FrontendLogWithBLOBs> logs = frontendLogMapper.selectByExampleWithBLOBs(example);
        PageInfo<FrontendLogWithBLOBs> pageInfo = new PageInfo<>(logs);

        log.info("前端日志查询完成, 共找到 {} 条数据", pageInfo.getTotal());

        return new PageImpl<>(pageInfo.getList(), PageRequest.of(page, size), pageInfo.getTotal());
    }

    private List<String> readNLinesBefore(File file, int numLines, LocalDateTime beforeTimestamp) throws IOException {
        LinkedList<String> lines = new LinkedList<>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                Matcher matcher = LOG_TIMESTAMP_PATTERN.matcher(currentLine);
                if (matcher.find()) {
                    LocalDateTime lineTimestamp = LocalDateTime.parse(matcher.group(1), LOG_DATE_FORMATTER);
                    if (lineTimestamp.isBefore(beforeTimestamp)) {
                        lines.add(currentLine);
                        if (lines.size() > numLines) {
                            lines.removeFirst();
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return new ArrayList<>(lines);
    }

    private List<String> readLastNLines(File file, int numLines) throws IOException {
        List<String> result = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) {
                return result;
            }
            long pos = fileLength - 1;
            int lineCount = 0;

            for (; pos >= 0 && lineCount < numLines; pos--) {
                raf.seek(pos);
                if (raf.readByte() == '\n') {
                    lineCount++;
                }
            }

            if (pos < 0) {
                pos = 0;
            } else {
                pos++;
            }

            raf.seek(pos);
            String line;
            while ((line = raf.readLine()) != null) {
                result.add(new String(line.getBytes("ISO-8859-1"), StandardCharsets.UTF_8));
            }
        }
        return result;
    }
}