FROM node:20-alpine
WORKDIR /app

# 安裝 pnpm
RUN npm install -g pnpm

# 先複製套件清單並安裝依賴（利用 Docker 緩存機制加速後續建置）
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

# 複製其餘所有原始碼
COPY . .

# 暴露 Vite 預設的 Port
EXPOSE 5173

# 啟動 Vite dev server，並加入 --host 參數允許外部（本機瀏覽器）連線
CMD ["pnpm", "run", "dev", "--host"]