# Phoenix HRM Automation Framework

## Overview
Phoenix HRM Automation Framework is a Human Resource Management application developed using the MERN stack (MongoDB, Express.js, React.js, and Node.js). The system streamlines HR processes such as employee management and user authentication.

---

## Technologies Used
- **Frontend:** React.js, Tailwind CSS
- **Backend:** Node.js, Express.js
- **Database:** MongoDB
- **Authentication:** JWT (JSON Web Token)

---

## Features
### 1. Authentication
- Login
- Registration
- Protected routes using JWT

### 2. Employee Management
- Add employee
- Update employee
- Delete employee
- View employee list

---

## Installation
### Prerequisites
- Node.js
- MongoDB
- Git

### Steps to Run the Project
```bash
# Clone repository
git clone <repo-url>

# Install backend dependencies
cd backend
npm install

# Start backend server
npm start

# Install frontend dependencies
cd ../frontend
npm install

# Start frontend client
npm start
```

---

## Folder Structure
```
Phoenix HRM
│
├── backend
│   ├── controllers
│   ├── models
│   ├── routes
│   ├── middleware
│   └── server.js
│
└── frontend
    ├── src
    │   ├── components
    │   ├── pages
    │   ├── context
    │   └── App.js
    └── tailwind.config.js
```

---

## API Endpoints
### Auth Routes
```
POST /api/auth/register
POST /api/auth/login
```

### Employee Routes
```
GET    /api/employees
POST   /api/employees
PUT    /api/employees/:id
DELETE /api/employees/:id
```

---

## Future Enhancements
- Role-based access
- Attendance management
- Payroll module
- Leave management system

---

#### Reporting Issues
- Ensure write permissions for report directories
- Check available disk space for screenshots
- Verify report paths in configuration

## 📈 Performance Considerations

- **Parallel Execution**: TestNG supports parallel test execution
- **Resource Management**: Automatic WebDriver cleanup
- **Screenshot Optimization**: Captured only on failures by default
- **Log Management**: Automatic log rotation and cleanup
- **Memory Management**: Efficient object lifecycle management

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Make changes and add tests
4. Run test suite: `mvn clean test`
5. Commit changes: `git commit -m "Add new feature"`
6. Push to branch: `git push origin feature/new-feature`
7. Create Pull Request

### Code Standards
- Follow existing code formatting and structure
- Add comprehensive JavaDoc comments
- Include unit tests for new utilities
- Update README for new features
- Maintain test coverage above 80%

## 📝 Documentation

- **API Documentation**: Auto-generated from test execution
- **Test Reports**: HTML reports with detailed execution information
- **JavaDoc**: Comprehensive inline code documentation


## 👥 Authors

**Ayan kumar Dash** - *Initial work and framework design*

- **Ayan kumar Dash** - *Initial work and framework design*
