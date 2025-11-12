# 🦅 Phoenix HRM Automation Framework  

![Build Status](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)
![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-F7DF1E?style=flat-square&logo=javascript&logoColor=black)
![Cypress](https://img.shields.io/badge/Cypress-Testing-17202C?style=flat-square&logo=cypress&logoColor=white)
![Mocha](https://img.shields.io/badge/Test%20Runner-Mocha-8D6748?style=flat-square&logo=mocha&logoColor=white)
![Chai](https://img.shields.io/badge/Assertion-Chai-A30701?style=flat-square&logo=chai&logoColor=white)
![POM](https://img.shields.io/badge/Design-POM-yellow?style=flat-square)
![GitHub](https://img.shields.io/badge/Version%20Control-GitHub-181717?style=flat-square&logo=github)

---

## 💼 Phoenix HRM Test Automation  

**Phoenix HRM Automation** is a JavaScript-based **end-to-end testing framework** designed to validate the core HR operations of a modern web application.  
It focuses on building **scalable, maintainable, and readable test architecture** using **Cypress** and the **Page Object Model (POM)** design pattern.

---

## 📋 Overview  

| Type | Description |
|------|--------------|
| 🌐 **UI Functional Tests** | Validate login, employee creation, and leave approval workflows |
| ⚙️ **Design Pattern** | Page Object Model for scalability |
| 📊 **Reports** | Auto-generated Cypress HTML reports with screenshots |
| 🔁 **Reusable Components** | Commands and custom utilities for modularity |
| 💬 **Assertions** | BDD-style assertions using Chai |

---

## ✨ Key Highlights  

✅ End-to-End HR flow coverage (Login → Employee → Leave)  
✅ Modular **Page Object Model (POM)** structure  
✅ Configurable **environment setup** via `cypress.config.js`  
✅ Built-in **retry mechanism & waits** for stable execution  
✅ **Screenshots & Videos** for failed test cases  
✅ **Cross-browser testing** with Chrome & Edge  

---

## 🧰 Tech Stack  

| Category | Tools / Frameworks |
|-----------|--------------------|
| **Language** | JavaScript (ES6+) |
| **Test Runner** | Cypress |
| **Assertion Library** | Chai |
| **Design Pattern** | Page Object Model (POM) |
| **Reporting** | Mochawesome / Allure Reports |
| **Version Control** | Git & GitHub |
| **CI/CD Integration** | GitHub Actions (optional) |

---

## ⚙️ Installation & Setup  

### 🧾 Prerequisites  
- Node.js (v16 or later)  
- npm or yarn  
- Git  

### 📦 Setup Commands  

```bash
# Clone Repository
git clone https://github.com/AyanKumarDash01/Phoenix-HRM-Automation.git
cd Phoenix-HRM-Automation

# Install Dependencies
npm install

# Run all Cypress tests in headless mode
npx cypress run

# Open Cypress test runner (GUI mode)
npx cypress open

# Generate Mochawesome report
npm run report
