# 🚀 Настройка Redis для AdaptixBot

## 📋 **Требования**

AdaptixBot использует Redis для хранения сессий пользователей. Если Redis недоступен, система автоматически переключится на хранение в памяти.

## 🛠️ **Установка Redis**

### Windows:
1. Скачайте Redis для Windows: https://github.com/microsoftarchive/redis/releases
2. Распакуйте архив
3. Запустите `redis-server.exe`

### Linux/macOS:
```bash
# Ubuntu/Debian
sudo apt-get install redis-server

# macOS (с Homebrew)
brew install redis

# Запуск
redis-server
```

## 🔧 **Проверка работы Redis**

```bash
# Проверка подключения
redis-cli ping
# Должно вернуть: PONG
```

## ⚙️ **Настройки по умолчанию**

- **Host:** localhost
- **Port:** 6379
- **Connection Pool:** 5-20 соединений
- **TTL сессий:** 24 часа

## 🚨 **Если Redis недоступен**

Система автоматически переключится на хранение сессий в памяти:
- ✅ Бот будет работать
- ⚠️ Сессии будут теряться при перезапуске
- ⚠️ Нет персистентности между инстансами

## 📊 **Мониторинг Redis**

После запуска бота проверьте:
- http://localhost:8080/health - статус Redis
- http://localhost:8080/metrics - метрики Redis соединений

## 🔧 **Альтернативные настройки**

Если Redis запущен на другом порту, измените в `RedisManager.java`:
```java
private static final String REDIS_HOST = "localhost";
private static final int REDIS_PORT = 6379; // Ваш порт
```
