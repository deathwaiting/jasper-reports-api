-- Create the employee table
CREATE TABLE IF NOT EXISTS employee (
    id IDENTITY PRIMARY KEY,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100) UNIQUE,
    hire_date DATE,
    job_title VARCHAR(50),
    salary DECIMAL(8, 2),
    department_id INT
);

-- Insert 10 rows into the employee table
INSERT INTO employee (first_name, last_name, email, hire_date, job_title, salary, department_id)
VALUES
('John', 'Doe', 'john.doe@example.com', '2020-01-01', 'Software Engineer', 70000.00, 2),
('Jane', 'Smith', 'jane.smith@example.com', '2019-06-15', 'Data Analyst', 65000.00, 2),
('Michael', 'Brown', 'michael.brown@example.com', '2018-11-20', 'Product Manager', 90000.00, 1),
('Linda', 'Johnson', 'linda.johnson@example.com', '2017-02-28', 'UX Designer', 75000.00, 2),
('Robert', 'Williams', 'robert.williams@example.com', '2016-09-30', 'Backend Developer', 85000.00, 2),
('Jessica', 'Miller', 'jessica.miller@example.com', '2015-08-25', 'Frontend Developer', 80000.00, 2),
('James', 'Davis', 'james.davis@example.com', '2014-07-14', 'DevOps Engineer', 75000.00, 2),
('Patricia', 'Wilson', 'patricia.wilson@example.com', '2013-06-03', 'QA Tester', 60000.00, 2),
('Richard', 'Moore', 'richard.moore@example.com', '2012-04-22', 'Business Analyst', 70000.00, 1),
('Jennifer', 'Anderson', 'jennifer.anderson@example.com', '2011-03-11', 'Marketing Specialist', 55000.00, 1);
