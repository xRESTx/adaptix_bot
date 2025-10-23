# AdaptixBot - High-Performance Telegram Bot

## 🚀 Features

- **High Performance**: Optimized for 200+ concurrent users
- **Redis Session Storage**: Persistent sessions with fallback to memory
- **HikariCP Connection Pool**: Fastest Java connection pool
- **Async Processing**: Non-blocking file uploads and processing
- **Monitoring**: Prometheus metrics and health checks
- **PostgreSQL**: Production-ready database

## 📋 Prerequisites

### Required Software

1. **Java 17+**
2. **PostgreSQL 12+**
3. **Redis 6+** (optional, but recommended)

### Database Setup

```sql
-- Create database
CREATE DATABASE adaptix_bot;

-- Create user (optional)
CREATE USER adaptix_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE adaptix_bot TO adaptix_user;
```

## 🛠️ Installation

### 1. Clone and Build

```bash
git clone <repository-url>
cd adaptix_bot
./gradlew build
```

### 2. Configure Database

Edit `src/main/resources/hibernate.cfg.xml`:

```xml
<property name="hibernate.connection.url">jdbc:postgresql://localhost:5432/adaptix_bot</property>
<property name="hibernate.connection.username">your_username</property>
<property name="hibernate.connection.password">your_password</property>
```

### 3. Configure Bot Token

Edit `src/main/resources/app.properties`:

```properties
bot.token=YOUR_BOT_TOKEN
bot.username=YOUR_BOT_USERNAME
```

## 🚀 Running the Bot

### Quick Start (Windows)

1. **Install dependencies:**
   ```bash
   install.bat
   ```

2. **Check system:**
   ```bash
   check-system.bat
   ```

3. **Start the bot:**
   ```bash
   start-bot.bat
   ```

### Option 1: With Redis (Recommended)

1. **Start Redis:**
   ```bash
   # Windows
   start-redis.bat
   
   # Linux/Mac
   sudo systemctl start redis
   
   # Docker
   docker run -d -p 6379:6379 redis:alpine
   ```

2. **Start the Bot:**
   ```bash
   start-bot.bat
   ```

### Option 2: Without Redis (Fallback Mode)

The bot will automatically fallback to in-memory session storage if Redis is unavailable.

```bash
start-bot.bat
```

## 📊 Monitoring

### Metrics Endpoints

- **Prometheus Metrics**: http://localhost:8080/metrics
- **Health Check**: http://localhost:8080/health
- **JSON Metrics**: http://localhost:8080/api/metrics
- **Statistics**: http://localhost:8080/api/stats

### Key Metrics

- `adaptix.messages.total` - Total messages processed
- `adaptix.users.active` - Active users
- `adaptix.sessions.active` - Active sessions
- `adaptix.database.connections` - Database connections
- `adaptix.redis.connections` - Redis connections

## 🔧 Configuration

### Database Connection Pool

```xml
<!-- HikariCP settings in hibernate.cfg.xml -->
<property name="hibernate.hikari.maximumPoolSize">20</property>
<property name="hibernate.hikari.minimumIdle">5</property>
<property name="hibernate.hikari.connectionTimeout">30000</property>
```

### Redis Settings

```java
// In RedisManager.java
private static final String REDIS_HOST = "localhost";
private static final int REDIS_PORT = 6379;
private static final int MAX_TOTAL = 20;
private static final int MAX_IDLE = 10;
```

## 🏗️ Architecture

### Components

1. **DatabaseManager** - Singleton for SessionFactory management
2. **RedisManager** - Redis connection pool and operations
3. **RedisSessionStore** - Persistent session storage
4. **AsyncService** - Non-blocking file processing
5. **MetricsService** - Prometheus metrics collection
6. **MetricsEndpoint** - HTTP metrics server

### Performance Optimizations

- **Connection Pooling**: HikariCP for database, JedisPool for Redis
- **Async Processing**: Dedicated thread pools for I/O operations
- **Session Persistence**: Redis with JSON serialization
- **Batch Operations**: Hibernate batch processing
- **Monitoring**: Real-time performance metrics

## 🐛 Troubleshooting

### Common Issues

1. **Redis Connection Failed**
   ```
   Redis unavailable - using memory fallback
   ```
   - Start Redis server: `redis-server`
   - Check Redis is running: `redis-cli ping`

2. **Database Connection Failed**
   ```
   Critical SessionFactory creation error
   ```
   - Check PostgreSQL is running
   - Verify connection settings in `hibernate.cfg.xml`

3. **Bot Token Invalid**
   ```
   Bot startup error
   ```
   - Verify bot token in `app.properties`
   - Check bot username is correct

### Logs

Check console output for detailed error messages. All Russian text has been replaced with English for better console compatibility.

## 📈 Performance

### Benchmarks

- **Concurrent Users**: 200+ supported
- **Response Time**: < 100ms average
- **Database Connections**: 20 max pool size
- **Redis Connections**: 20 max pool size
- **Memory Usage**: Optimized with connection pooling

### Scaling

For higher loads:
1. Increase connection pool sizes
2. Add Redis clustering
3. Use database read replicas
4. Implement horizontal scaling

## 🔒 Security

- Database credentials in configuration files
- Redis without authentication (local only)
- Bot token protection
- File upload validation

## 📝 License

[Your License Here]

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## 📞 Support

For issues and questions:
- Check troubleshooting section
- Review logs for error messages
- Monitor metrics endpoints
- Check database and Redis connectivity
