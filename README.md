# Co-working Space Management and Booking System

A Java-based desktop application designed to manage co-working spaces, memberships, workspace bookings, check-ins, payments, invoices, and reports. The system provides an organised platform for managing co-working space operations efficiently.

## Features

- **User Authentication:** Secure login and role-based access.
- **Dashboard:** Overview of members, bookings, workspaces, and revenue.
- **Member Management:** Add, view, update, search, and manage members.
- **Workspace Management:** Manage desks, offices, and meeting rooms.
- **Booking Management:** Create, view, and manage workspace reservations.
- **Check-in Management:** Record and track member check-ins.
- **Membership Plans:** Manage membership plans and subscriptions.
- **Payment Management:** Record and manage member payments.
- **Invoice Management:** Generate and manage invoices.
- **Reports:** View business reports and revenue information.
- **Audit Logs:** Track important system activities.

## Technology Stack

- **Programming Language:** Java
- **Desktop UI:** JavaFX
- **Build Tool:** Apache Maven
- **Database:** PostgreSQL
- **Database Connectivity:** JDBC
- **Connection Pooling:** HikariCP
- **Testing:** JUnit 5
- **IDE:** IntelliJ IDEA

## Project Architecture

The project follows a layered architecture to maintain separation of concerns and improve code organisation.

```text
com.time.coworking
│
├── controller
├── dto
├── enums
├── exception
├── model
├── repository
├── service
├── ui
│
├── Launcher.java
├── Main.java
└── Session.java
```

### Architecture Layers

- **Controller:** Handles application operations and coordinates requests.
- **Model:** Contains the application's core data entities.
- **DTO:** Transfers data between different application layers.
- **Repository:** Handles database operations.
- **Service:** Contains business logic.
- **UI:** Provides the application's graphical user interface.
- **Exception:** Handles application-specific errors.
- **Enums:** Defines fixed values used throughout the system.

## Prerequisites

Make sure you have installed:

- JDK 21
- Apache Maven 3.8+
- PostgreSQL
- IntelliJ IDEA or another Java IDE

## Installation and Setup

### 1. Clone the Repository

```bash
git clone https://github.com/AbdulRasheedAbro/co-working-space-management-and-booking-system.git
```

### 2. Open the Project

Open the cloned project in IntelliJ IDEA.

### 3. Configure the Database

Create a PostgreSQL database and execute the SQL scripts provided in the project resources.

Copy the example database configuration file:

```text
db.properties.example
```

Rename the copied file to:

```text
db.properties
```

Update the database URL, username, and password according to your PostgreSQL configuration.

### 4. Run the Application

Execute the following Maven command:

```bash
mvn clean javafx:run
```

### 5. Build the Project

To build the application, execute:

```bash
mvn clean package
```

## Application Modules

| Module | Description |
|---|---|
| Dashboard | Displays system statistics |
| Members | Manages member information |
| Workspaces | Manages available workspaces |
| Bookings | Handles workspace reservations |
| Check-ins | Records member attendance |
| Membership Plans | Manages membership packages |
| Payments | Tracks payment transactions |
| Invoices | Manages billing records |
| Reports | Provides business reports |
| Audit Logs | Tracks system activities |

## Security

Database credentials and sensitive configuration files should not be committed to GitHub.

The `.gitignore` file excludes local database credentials and unnecessary development files.

## Future Improvements

- Online workspace booking.
- Email notifications.
- Advanced analytics and reporting.
- Improved booking availability management.
- Cloud-based deployment.

## Author

**Abdul Rasheed Abro**

GitHub: [AbdulRasheedAbro](https://github.com/AbdulRasheedAbro)

## License

This project is developed for educational and software development purposes.
