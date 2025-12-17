# Finance Tracker - Personal Budget Management System

A comprehensive JavaFX-based financial management application with Supabase backend for tracking income, expenses, budgets, bills, investments, and analyzing spending patterns.

## 🚀 Features

### Core Functionality
- **Monthly Income Tracking**: Record and manage multiple income sources
- **Expense Management**: Categorize and track all expenses
- **Budget Planning**: Set spending limits with real-time alerts
- **Bills & Subscriptions**: Track recurring payments and get reminders
- **Investment Tracking**: Monitor portfolio performance
- **Analytics & Reports**: Visualize spending patterns with charts
- **Transfer Management**: Track money movement between accounts
- **Custom Categories**: Create personalized income/expense categories

### Technical Features
- Secure authentication with Supabase
- PostgreSQL database with Row Level Security
- Real-time data synchronization
- Responsive JavaFX UI
- MVC architecture
- Professional reports (PDF/Excel export)

## 📋 Prerequisites

- **Java Development Kit (JDK)** 11 or higher
- **Apache Maven** 3.6+ or **Gradle** 6.0+
- **Supabase Account** (for backend database)
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

## 🛠️ Setup Instructions

### 1. Clone the Repository
```bash
git clone <repository-url>
cd FinanceTracker
```

### 2. Set Up Supabase

1. Create a free account at [supabase.com](https://supabase.com)
2. Create a new project
3. Go to SQL Editor and run the `database_schema.sql` file
4. Get your project credentials:
   - Project URL: `https://your-project.supabase.co`
   - Anon Key: Found in Settings > API
   - Database Password: Set during project creation

### 3. Configure Application

Edit `src/main/resources/application.properties`:

```properties
# Supabase Configuration
supabase.url=https://your-project.supabase.co
supabase.key=your-anon-key-here
supabase.jwt.secret=your-jwt-secret

# Database Configuration
db.url=jdbc:postgresql://db.your-project.supabase.co:5432/postgres
db.username=postgres
db.password=your-database-password
db.schema=public
```

### 4. Build the Project

Using Maven:
```bash
mvn clean install
```

Using Gradle:
```bash
gradle clean build
```

### 5. Run the Application

Using Maven:
```bash
mvn javafx:run
```

Using your IDE:
- Run the `MainApp.java` class

Using compiled JAR:
```bash
java -jar target/finance-tracker-1.0.0.jar
```

## 📁 Project Structure

```
FinanceTracker/
├── src/
│   ├── main/
│   │   ├── java/com/financetracker/
│   │   │   ├── model/              # Data models (User, Income, Expense, etc.)
│   │   │   ├── controller/         # JavaFX controllers
│   │   │   ├── service/           # Business logic & database operations
│   │   │   ├── util/              # Utility classes (Config, Supabase client)
│   │   │   └── MainApp.java       # Application entry point
│   │   └── resources/
│   │       ├── fxml/              # JavaFX FXML layouts
│   │       ├── css/               # Stylesheets
│   │       └── application.properties
│   └── test/
│       └── java/                  # Unit tests
├── pom.xml                        # Maven configuration
├── database_schema.sql            # Supabase database schema
└── README.md
```

## 🗄️ Database Schema

### Key Tables
- **users** - User accounts and authentication
- **categories** - Income/Expense categories
- **income** - Income transactions
- **expenses** - Expense records
- **budgets** - Budget limits and alerts
- **bills_subscriptions** - Recurring payments
- **accounts** - Financial accounts
- **transfers** - Account transfers
- **investments** - Investment portfolio
- **budget_alerts** - Budget notifications

## 🔐 Security Features

- Password hashing with BCrypt
- Row Level Security (RLS) in Supabase
- JWT token-based authentication
- Secure database connections
- User data isolation

## 🎨 UI Components

- Login/Signup screens
- Dashboard with financial summary
- Income/Expense management forms
- Budget tracking with progress bars
- Bills calendar view
- Analytics charts (Pie, Bar, Line)
- Settings and preferences

## 📊 Technologies Used

- **Frontend**: JavaFX 17
- **Backend**: Supabase (PostgreSQL)
- **Database**: PostgreSQL with JDBC
- **Charts**: JFreeChart
- **Reports**: iText (PDF), Apache POI (Excel)
- **Logging**: SLF4J + Logback
- **Build Tool**: Maven
- **Authentication**: BCrypt

## 🚧 Development Roadmap

### Phase 1 (Weeks 1-3): Foundation
- [x] Project setup and configuration
- [x] Database schema design
- [x] Authentication system
- [ ] Basic CRUD operations

### Phase 2 (Weeks 4-6): Core Features
- [ ] Income/Expense tracking
- [ ] Budget management
- [ ] Bills & subscriptions
- [ ] Category management

### Phase 3 (Weeks 7-9): Advanced Features
- [ ] Analytics and charts
- [ ] Investment tracking
- [ ] Transfer management
- [ ] Report generation

### Phase 4 (Week 10): Polish
- [ ] UI/UX improvements
- [ ] Testing and bug fixes
- [ ] Documentation
- [ ] Deployment

## 🧪 Testing

Run unit tests:
```bash
mvn test
```

## 📝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 👥 Team

- **Team Size**: 4-5 members
- **Timeline**: 10 weeks
- **Role Distribution**: 
  - Backend Developer
  - Frontend Developer
  - Database Administrator
  - UI/UX Designer
  - QA Tester

## 📄 License

This project is licensed under the MIT License.

## 🐛 Known Issues

- Supabase Java SDK integration (using REST API as workaround)
- Real-time updates not yet implemented
- Mobile responsiveness pending

## 📞 Support

For issues and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

## 🎯 Future Enhancements

- Mobile app (Android/iOS)
- Multi-currency support
- Automatic bank synchronization
- AI-powered spending predictions
- Receipt scanning with OCR
- Family account sharing
- Cloud backup and sync


## GITHUB REPO: https://github.com/Jack-723/Personal-Finance-Tracker-project.git
---

**Version**: 1.0.0  
**Last Updated**: November 2024  
**Status**: Active Development
