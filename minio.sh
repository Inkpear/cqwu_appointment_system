docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name minio \
  -v /data/minio/data:/data:Z \
  -v /data/minio/config:/root/.minio:Z \
  -e "MINIO_ROOT_USER=root" \
  -e "MINIO_ROOT_PASSWORD=admin123" \
  --restart=always \
  minio/minio server /data --console-address ":9001"
