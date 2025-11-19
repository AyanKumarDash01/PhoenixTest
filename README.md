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

## License
This project is licensed under the MIT License.
