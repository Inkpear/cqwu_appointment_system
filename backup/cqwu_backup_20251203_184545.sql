-- MySQL dump 10.13  Distrib 8.0.44, for Linux (x86_64)
--
-- Host: localhost    Database: cqwu
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `cqwu`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `cqwu` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `cqwu`;

--
-- Table structure for table `app_files`
--

DROP TABLE IF EXISTS `app_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_files` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `related_entity_id` bigint DEFAULT NULL COMMENT 'å…³è”çš„ä¸šåŠ¡å®žä½“ID (venue.id æˆ– reservations.id)ï¼Œåˆ›å»ºæ—¶å¯ä¸ºNULL',
  `entity_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ä¸šåŠ¡å®žä½“ç±»åž‹ (ä¾‹å¦‚: VENUE, RESERVATION)',
  `object_name` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ–‡ä»¶åœ¨MinIOä¸­çš„å¯¹è±¡å',
  `file_category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ–‡ä»¶åˆ†ç±» (ä¾‹å¦‚: CAROUSEL, ATTACHMENT)',
  `original_filename` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int DEFAULT '0' COMMENT 'æ˜¾ç¤ºé¡ºåº (ä»…ç”¨äºŽVENUEçš„CAROUSELç­‰)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_entity` (`related_entity_id`,`entity_type`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å…¨å±€æ–‡ä»¶å…³è”è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `app_files`
--

LOCK TABLES `app_files` WRITE;
/*!40000 ALTER TABLE `app_files` DISABLE KEYS */;
INSERT INTO `app_files` VALUES (3,42,'RESERVATION','reservation/temp_04cb5d23f13841d98ff521a008271d57/attachment/5f89454eb4a34d98a53216370edf3986.gif','ATTACHMENT','1753963453099.gif',0,'2025-12-02 04:06:37'),(4,42,'RESERVATION','reservation/temp_04cb5d23f13841d98ff521a008271d57/attachment/b00acccd7f004e4bb6106d4975890cb2.jpeg','ATTACHMENT','archetto.jpeg',0,'2025-12-02 04:06:37'),(5,43,'RESERVATION','reservation/temp_c01883caccf14f2f9a288c2429a2c0a1/attachment/24d6d4baf4104985a8c90501734934d0.jpg','ATTACHMENT','Image_1754047909809.jpg',0,'2025-12-02 06:27:37'),(7,NULL,'VENUE','venue/temp_885f5e1cc8d24e9d9647d932843c036d/carousel/da04f2be19e0428eadf51b16a81e1499.jpeg','CAROUSEL','archetto.jpeg',0,'2025-12-02 13:18:46'),(8,NULL,'VENUE','venue/temp_885f5e1cc8d24e9d9647d932843c036d/carousel/ac8b5d737a36489a9ab2163ccd9e99a5.jpg','CAROUSEL','arknights_summer-slim.jpg',0,'2025-12-02 13:18:46'),(9,NULL,'VENUE','venue/temp_3ec953e2bf184c8483598010a70ad92f/attachment/ccfa450997ce4a83af3a94aea2485e8f.jpg','ATTACHMENT','arknights_summer-slim.jpg',0,'2025-12-02 13:21:29'),(10,NULL,'VENUE','venue/temp_3b9d841c58e440abafc2943d9baf3663/attachment/09905bb5bb3840f1a9030d0d40de3b96.jpg','ATTACHMENT','arknights_summer-slim.jpg',0,'2025-12-02 13:21:45'),(11,533,'VENUE','venue/temp_7d38a567ff7343f39a1729b312cdb268/carousel/c764e5239def4112ac2a8cfe23c461d2.gif','CAROUSEL','c764e5239def4112ac2a8cfe23c461d2.gif',0,'2025-12-02 14:07:49'),(12,533,'VENUE','venue/temp_756084bc81794683a1500d2e964795e0/attachment/09f082196d654557a078f3549fee4608.png','ATTACHMENT','09f082196d654557a078f3549fee4608.png',0,'2025-12-02 14:24:43'),(13,533,'VENUE','venue/temp_9acea8b3b21d4d3e889d8e64de53ed32/attachment/64986c02610c4ab8ba1e518865ce7209.png','ATTACHMENT','64986c02610c4ab8ba1e518865ce7209.png',0,'2025-12-02 14:25:07'),(14,533,'VENUE','venue/temp_8a3c32ec38b84e64bdeafe680b463990/attachment/98a340dd025f42119aedbd3c1db9912d.png','ATTACHMENT','98a340dd025f42119aedbd3c1db9912d.png',0,'2025-12-02 14:25:08'),(15,533,'VENUE','venue/temp_2b78154717ad49b7978276479d2782c2/attachment/629f9666573e438c8b5b6abb84c2af56.png','ATTACHMENT','629f9666573e438c8b5b6abb84c2af56.png',0,'2025-12-02 14:25:08'),(16,46,'RESERVATION','reservation/temp_7de7edf21215460ca8340ad06ddd0368/attachment/7b13b68ee53c4ffb9726999581ce95ed.gif','ATTACHMENT','7b13b68ee53c4ffb9726999581ce95ed.gif',0,'2025-12-02 14:35:02');
/*!40000 ALTER TABLE `app_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `frontend_logs`
--

DROP TABLE IF EXISTS `frontend_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `frontend_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `log_level` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ—¥å¿—çº§åˆ« (å¦‚ INFO, WARN, ERROR)',
  `log_message` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ—¥å¿—å†…å®¹',
  `log_timestamp` datetime NOT NULL COMMENT 'å‰ç«¯ç”Ÿæˆæ—¥å¿—çš„æ—¶é—´',
  `user_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ä¸ŠæŠ¥æ—¥å¿—çš„ç”¨æˆ·å (å­¦å·/å·¥å·)',
  `user_agent` text COLLATE utf8mb4_unicode_ci COMMENT 'ç”¨æˆ·çš„æµè§ˆå™¨æˆ–å®¢æˆ·ç«¯ä¿¡æ¯',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'æ—¥å¿—å…¥åº“æ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_log_level` (`log_level`),
  KEY `idx_log_timestamp` (`log_timestamp`)
) ENGINE=InnoDB AUTO_INCREMENT=276 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å‰ç«¯ä¸ŠæŠ¥æ—¥å¿—è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `frontend_logs`
--

LOCK TABLES `frontend_logs` WRITE;
/*!40000 ALTER TABLE `frontend_logs` DISABLE KEYS */;
INSERT INTO `frontend_logs` VALUES (123,'ERROR','API 请求失败 [object Object]\nStacktrace:\n    at http://localhost:5173/src/utils/request.ts:85:9\n    at async Axios.request (http://localhost:5173/node_modules/.vite/deps/axios.js?v=8ca575be:2197:14)\n    at async fetchSystemInfo (http://localhost:5173/src/views/dashboard/index.vue:51:21)','2025-11-13 05:09:41','202358314053','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-11-13 05:09:40'),(256,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:17','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(257,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:21','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(258,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:25','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(259,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:29','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(260,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:33','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(261,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:37','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(262,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:41','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(263,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:45','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(264,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:49','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(265,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:53','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(266,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:03:57','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(267,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:04:01','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(268,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:04:05','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(269,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:04:09','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(270,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:04:13','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(271,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:04:17','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(272,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:04:21','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(273,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:04:25','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(274,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:04:29','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02'),(275,'ERROR','SSE 连接错误 [object Object]\nStacktrace:\n    at EventSourcePolyfill.onerror (http://localhost:5173/src/utils/sseService.ts:109:11)\n    at fire (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:608:15)\n    at onStart (http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:686:15)\n    at http://localhost:5173/node_modules/.vite/deps/event-source-polyfill.js?v=cbf85022:463:11','2025-12-02 15:04:33','202358314046','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0','2025-12-02 15:05:02');
/*!40000 ALTER TABLE `frontend_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservations`
--

DROP TABLE IF EXISTS `reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservations` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'é¢„çº¦è®°å½•çš„ä¸»é”®ID',
  `venue_id` bigint NOT NULL COMMENT 'åœºæ‰€ID, å†—ä½™å­—æ®µç”¨äºŽå¿«é€ŸæŸ¥è¯¢',
  `start_datetime` datetime NOT NULL COMMENT 'ç”³è¯·çš„å¼€å§‹æ—¶é—´',
  `end_datetime` datetime NOT NULL COMMENT 'ç”³è¯·çš„ç»“æŸæ—¶é—´',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ç”³è¯·çš„æ ‡é¢˜',
  `purpose` text COLLATE utf8mb4_unicode_ci COMMENT 'é¢„çº¦çš„è¯¦ç»†äº‹ç”±/ç›®çš„',
  `time_block_id` bigint DEFAULT NULL COMMENT 'å…³è”åˆ° time_blocks è¡¨çš„ä¸»é”®ID (ä»…åœ¨CONFIRMEDçŠ¶æ€ä¸‹æœ‰æ•ˆ)',
  `creator_user_id` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'åˆ›å»ºè€…ç”¨æˆ·ID (å­¦å·/å·¥å·)',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'çŠ¶æ€ (PENDING, CONFIRMED, REJECTED, CANCELLED, EXPIRED)',
  `appointment_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'é¢„çº¦ç¡®è®¤ç ï¼Œåœ¨çŠ¶æ€å˜ä¸º CONFIRMED æ—¶ç”Ÿæˆ',
  `approver_user_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å®¡æ‰¹äººç”¨æˆ·ID',
  `approval_time` datetime DEFAULT NULL COMMENT 'å®¡æ‰¹æ“ä½œæ—¶é—´',
  `approval_comment` text COLLATE utf8mb4_unicode_ci COMMENT 'å®¡æ‰¹æ„è§',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ç”³è¯·åˆ›å»ºæ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_time_block_id` (`time_block_id`),
  UNIQUE KEY `uk_appointment_code` (`appointment_code`),
  KEY `idx_creator_user_id` (`creator_user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='é¢„çº¦ä¸šåŠ¡è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservations`
--

LOCK TABLES `reservations` WRITE;
/*!40000 ALTER TABLE `reservations` DISABLE KEYS */;
INSERT INTO `reservations` VALUES (9,533,'2025-11-13 08:00:00','2025-11-13 19:00:00','论文答辩','我们需要一个教室进行论文答辩',6,'202358314046','CANCELLED','8fb1e090418149a8','SYSTEM','2025-11-12 19:23:00',NULL,'2025-11-12 19:22:32'),(10,533,'2025-11-12 19:34:00','2025-11-12 20:34:00','论文答辩','需要一个教室进行论文答辩',NULL,'202358314046','EXPIRED',NULL,NULL,NULL,NULL,'2025-11-12 19:33:18'),(11,533,'2025-11-12 19:40:00','2025-11-12 19:50:00','论文答辩','论文答辩',7,'202358314046','COMPLETED','26f01c5638714029','SYSTEM','2025-11-12 19:38:00',NULL,'2025-11-12 19:37:24'),(12,533,'2025-11-13 07:02:00','2025-11-13 07:04:00','123','123',8,'202358314053','COMPLETED','1ffefbf31e8f4761','SYSTEM','2025-11-12 23:53:00',NULL,'2025-11-12 23:52:00'),(13,533,'2025-11-13 23:58:00','2025-11-13 23:59:00','测试','测试这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长文本这是长这是长文本',9,'202358314046','COMPLETED','379b05d28531468a','SYSTEM','2025-11-12 23:57:00',NULL,'2025-11-12 23:56:56'),(14,533,'2025-11-13 13:00:00','2025-11-13 14:00:00','项目讨论','需要一个教室进行项目讨论',10,'202358314046','COMPLETED','cca29f6ff2df46c3','SYSTEM','2025-11-13 10:26:00',NULL,'2025-11-13 10:25:38'),(42,533,'2025-12-03 08:59:00','2025-12-03 10:00:00','测试上传','测试上传',53,'202358314046','COMPLETED','725270fe81df4c6f','202358314046','2025-12-02 15:15:06','审批通过','2025-12-02 12:06:38'),(43,533,'2025-12-04 06:00:00','2025-12-06 08:00:00','测试上传文件','测试上传文件',54,'202358314046','CONFIRMED','7ea30febf95246ec','202358314046','2025-12-02 15:15:08','审批通过','2025-12-02 14:27:38'),(44,533,'2025-12-24 09:45:00','2025-12-24 09:46:00','部门开会','部门需要讨论下周的活动安排',55,'202358314046','CONFIRMED','593fa5a0368e4336','202358314046','2025-12-02 15:15:10','审批通过','2025-12-02 14:33:43'),(45,533,'2025-12-25 09:45:00','2025-12-25 09:46:00','部门开会','部门需要讨论下周的活动安排',NULL,'202358314046','PENDING',NULL,NULL,NULL,NULL,'2025-12-02 22:27:49'),(46,533,'2025-12-26 09:45:00','2025-12-26 09:46:00','部门开会','部门需要讨论下周的活动安排',NULL,'202358314046','PENDING',NULL,NULL,NULL,NULL,'2025-12-02 22:35:03');
/*!40000 ALTER TABLE `reservations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `time_blocks`
--

DROP TABLE IF EXISTS `time_blocks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `time_blocks` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `venue_id` bigint NOT NULL COMMENT 'åœºæ‰€ID, å…³è” venue è¡¨',
  `start_datetime` datetime NOT NULL COMMENT 'å ç”¨å¼€å§‹æ—¶é—´',
  `end_datetime` datetime NOT NULL COMMENT 'å ç”¨ç»“æŸæ—¶é—´',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å ç”¨äº‹ä»¶çš„æ ‡é¢˜ (ä¾‹å¦‚: é«˜ç­‰æ•°å­¦, å­¦ç”Ÿæ´»åŠ¨é¢„çº¦, ç®¡é“ç»´ä¿®)',
  `block_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å ç”¨ç±»åž‹ (APPOINTMENT: é¢„çº¦, COURSE: è¯¾ç¨‹, MAINTENANCE: ç»´æŠ¤)',
  `reservation_id` bigint DEFAULT NULL COMMENT 'å¦‚æžœç±»åž‹æ˜¯APPOINTMENT, è¿™é‡Œåå‘å…³è”åˆ° reservations è¡¨',
  PRIMARY KEY (`id`),
  KEY `idx_venue_datetime` (`venue_id`,`start_datetime`,`end_datetime`),
  KEY `idx_reservation_id` (`reservation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='åœºæ‰€æ—¶é—´å ç”¨è¡¨ï¼ˆè°ƒåº¦æ ¸å¿ƒï¼‰';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `time_blocks`
--

LOCK TABLES `time_blocks` WRITE;
/*!40000 ALTER TABLE `time_blocks` DISABLE KEYS */;
INSERT INTO `time_blocks` VALUES (54,533,'2025-12-04 06:00:00','2025-12-06 08:00:00','测试上传文件','APPOINTMENT',43),(55,533,'2025-12-24 09:45:00','2025-12-24 09:46:00','部门开会','APPOINTMENT',44);
/*!40000 ALTER TABLE `time_blocks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ä¸»é”®ID, å­˜å‚¨å­¦å·æˆ–å·¥å·',
  `user_type` tinyint NOT NULL COMMENT 'ç”¨æˆ·ç±»åž‹ (1: å­¦ç”Ÿ, 2: æ•™å¸ˆ, 0: å…¶ä»–)',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'çœŸå®žå§“å',
  `sex` tinyint DEFAULT '0' COMMENT 'æ€§åˆ« (1: ç”·, 2: å¥³, 0: æœªçŸ¥)',
  `college` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ‰€å±žå­¦é™¢',
  `major` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ‰€å±žä¸“ä¸š',
  `role` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT 'è§’è‰² (ä¾‹å¦‚: user, admin)',
  `last_login_time` datetime DEFAULT NULL COMMENT 'æœ€åŽç™»å½•æ—¶é—´',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'è®°å½•åˆ›å»ºæ—¶é—´',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'è®°å½•æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_type` (`user_type`) USING BTREE,
  KEY `idx_role` (`role`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='ç”¨æˆ·ä¿¡æ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES ('202358314046',1,'inkpear',0,NULL,NULL,'ROOT','2025-12-02 22:07:32','2025-11-11 13:58:26','2025-12-02 14:07:32'),('202358314053',1,'yui',0,NULL,NULL,'ROOT','2025-11-13 12:55:49','2025-11-12 08:17:37','2025-11-13 04:55:48');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `venue`
--

DROP TABLE IF EXISTS `venue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `venue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'åœºæ‰€åç§°',
  `location` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ä½ç½®',
  `floor` int DEFAULT NULL COMMENT 'æ¥¼å±‚',
  `capacity` int DEFAULT '0' COMMENT 'å®¹é‡',
  `category_id` bigint DEFAULT NULL COMMENT 'åˆ†ç±»ID, å…³è” venue_category è¡¨',
  `equipment` text COLLATE utf8mb4_unicode_ci COMMENT 'è®¾å¤‡é…ç½® (ä¾‹å¦‚: æŠ•å½±ä»ª, ç™½æ¿, ç©ºè°ƒ)',
  `base_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE' COMMENT 'åŸºç¡€çŠ¶æ€ (ç”±ç®¡ç†å‘˜è®¾ç½®, å¦‚ AVAILABLE, MAINTENANCE, DISABLED)',
  `realtime_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNKNOWN' COMMENT 'å®žæ—¶çŠ¶æ€ (ç”±å®šæ—¶ä»»åŠ¡è®¡ç®—, å¦‚ AVAILABLE, OCCUPIED, CLOSED)ã€‚UNKNOWN ä¸ºåˆå§‹å€¼',
  `open_time` time DEFAULT NULL COMMENT 'æ¯æ—¥å¼€æ”¾æ—¶é—´ (ä¾‹å¦‚: 08:00:00)',
  `close_time` time DEFAULT NULL COMMENT 'æ¯æ—¥å…³é—­æ—¶é—´ (ä¾‹å¦‚: 22:00:00)',
  `requires_approval` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'é¢„çº¦æ˜¯å¦éœ€è¦äººå·¥å®¡æ‰¹ (TRUE: æ˜¯, FALSE: å¦)',
  `max_booking_duration_minutes` int DEFAULT NULL COMMENT 'å•æ¬¡é¢„çº¦æœ€é•¿æ—¶é•¿ï¼ˆåˆ†é’Ÿï¼‰ã€‚å¦‚æžœä¸ºNULLï¼Œåˆ™è¡¨ç¤ºæ— é™åˆ¶',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_base_status` (`base_status`),
  KEY `idx_realtime_status` (`realtime_status`)
) ENGINE=InnoDB AUTO_INCREMENT=782 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='åœºæ‰€ä¿¡æ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `venue`
--

LOCK TABLES `venue` WRITE;
/*!40000 ALTER TABLE `venue` DISABLE KEYS */;
INSERT INTO `venue` VALUES (533,'A101','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:59:59',1,NULL),(534,'B101','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(535,'A102','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(536,'B102','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(537,'A103','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(538,'B103','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(539,'A104','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(540,'B104','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(541,'A105','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(542,'B105','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(543,'A106','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(544,'B106','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(545,'A107','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(546,'B107','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(547,'A108','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',1,NULL),(548,'B108','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(549,'A109','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(550,'B109','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(551,'A110','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(552,'B110','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(553,'A111','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(554,'B111','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(555,'A201','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(556,'B201','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(557,'A202','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(558,'B202','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(559,'A203','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(560,'B203','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(561,'A204','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(562,'B204','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(563,'A205','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(564,'B205','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(565,'A206','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(566,'B206','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(567,'A207','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(568,'B207','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(569,'A208','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(570,'B208','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(571,'A209','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(572,'B209','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(573,'A210','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(574,'B210','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(575,'A211','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(576,'B211','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(577,'A301','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(578,'B301','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(579,'A302','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(580,'B302','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(581,'A303','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(582,'B303','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(583,'A304','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(584,'B304','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(585,'A305','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(586,'B305','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(587,'A306','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(588,'B306','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(589,'A307','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(590,'B307','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(591,'A308','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(592,'B308','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(593,'A309','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(594,'B309','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(595,'A310','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(596,'B310','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(597,'A311','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(598,'B311','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(599,'A401','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(600,'B401','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(601,'A402','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(602,'B402','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(603,'A403','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(604,'B403','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(605,'A404','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(606,'B404','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(607,'A405','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(608,'B405','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(609,'A406','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(610,'B406','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(611,'A407','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(612,'B407','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(613,'A408','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(614,'B408','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(615,'A409','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(616,'B409','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(617,'A410','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(618,'B410','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(619,'A411','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(620,'B411','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(621,'C101','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(622,'D101','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(623,'C102','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(624,'D102','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(625,'C103','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(626,'D103','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(627,'C104','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(628,'D104','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(629,'C105','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(630,'D105','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(631,'C106','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(632,'D106','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(633,'C107','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(634,'D107','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(635,'C108','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(636,'D108','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(637,'C109','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(638,'D109','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(639,'C110','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(640,'D110','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(641,'C111','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(642,'D111','格致楼',1,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(643,'C201','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(644,'D201','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(645,'C202','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(646,'D202','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(647,'C203','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(648,'D203','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(649,'C204','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(650,'D204','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(651,'C205','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(652,'D205','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(653,'C206','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(654,'D206','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(655,'C207','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(656,'D207','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(657,'C208','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(658,'D208','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(659,'C209','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(660,'D209','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(661,'C210','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(662,'D210','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(663,'C211','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(664,'D211','格致楼',2,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(665,'C301','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(666,'D301','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(667,'C302','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(668,'D302','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(669,'C303','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(670,'D303','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(671,'C304','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(672,'D304','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(673,'C305','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(674,'D305','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(675,'C306','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(676,'D306','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(677,'C307','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(678,'D307','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(679,'C308','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(680,'D308','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(681,'C309','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(682,'D309','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(683,'C310','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(684,'D310','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(685,'C311','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(686,'D311','格致楼',3,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(687,'C401','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(688,'D401','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(689,'C402','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(690,'D402','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(691,'C403','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(692,'D403','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(693,'C404','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(694,'D404','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(695,'C405','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(696,'D405','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(697,'C406','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(698,'D406','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(699,'C407','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(700,'D407','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(701,'C408','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(702,'D408','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(703,'C409','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(704,'D409','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(705,'C410','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(706,'D410','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(707,'C411','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL),(708,'D411','格致楼',4,64,1,'计算机 投影仪','AVAILABLE','AVAILABLE','07:00:00','23:00:00',0,NULL);
/*!40000 ALTER TABLE `venue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `venue_category`
--

DROP TABLE IF EXISTS `venue_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `venue_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'åˆ†ç±»åç§° (e.g., æ™®é€šæ•™å®¤, ä¼šè®®å®¤, å®žéªŒå®¤)',
  `usage_rules` text COLLATE utf8mb4_unicode_ci COMMENT 'ä½¿ç”¨è§„åˆ™æˆ–ç­–ç•¥æè¿°',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='åœºæ‰€åˆ†ç±»è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `venue_category`
--

LOCK TABLES `venue_category` WRITE;
/*!40000 ALTER TABLE `venue_category` DISABLE KEYS */;
INSERT INTO `venue_category` VALUES (1,'机房','允许学生，教师预约');
/*!40000 ALTER TABLE `venue_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `venue_category_rules`
--

DROP TABLE IF EXISTS `venue_category_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `venue_category_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL COMMENT 'å…³è”åˆ° venue_category è¡¨',
  `allowed_user_type` tinyint NOT NULL COMMENT 'å…è®¸çš„ç”¨æˆ·ç±»åž‹ (1:å­¦ç”Ÿ, 2:æ•™å¸ˆ, 0:å…¶ä»–)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_user_type` (`category_id`,`allowed_user_type`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='åœºæ‰€åˆ†ç±»å‡†å…¥è§„åˆ™è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `venue_category_rules`
--

LOCK TABLES `venue_category_rules` WRITE;
/*!40000 ALTER TABLE `venue_category_rules` DISABLE KEYS */;
INSERT INTO `venue_category_rules` VALUES (1,1,1),(2,1,2);
/*!40000 ALTER TABLE `venue_category_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'cqwu'
--

--
-- Dumping routines for database 'cqwu'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-03 10:45:45
