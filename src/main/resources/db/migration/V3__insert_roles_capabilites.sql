-- Insert roles
INSERT INTO roles (name)
VALUES
    ('ADMIN'),
    ('EMPLOYEE'),
    ('TEACHER');

-- Insert capabilities
INSERT INTO capabilities (name, description)
VALUES
    ('INSERT_TEACHER', 'Create a new teacher'),
    ('VIEW_TEACHERS', 'View teacher list and details'),
    ('VIEW_TEACHER', 'View teacher'),
    ('EDIT_TEACHER', 'Modify existing teacher'),
    ('DELETE_TEACHER', 'Remove a teacher'),
    ('VIEW_ONLY_TEACHER', 'View only own teacher details');

-- Assign capabilities to ADMIN (all capabilities)
INSERT INTO roles_capabilities (role_id, capability_id)
SELECT r.id, c.id
FROM roles r
JOIN capabilities c
WHERE r.name = 'ADMIN';

INSERT INTO roles_capabilities (role_id, capability_id)
SELECT r.id, c.id
FROM roles r
JOIN capabilities c
WHERE r.name = 'EMPLOYEE'
  AND c.name IN ('VIEW_TEACHERS', 'VIEW_TEACHER');

INSERT INTO roles_capabilities (role_id, capability_id)
SELECT r.id, c.id
FROM roles r
JOIN capabilities c
WHERE r.name = 'TEACHER'
AND c.name IN ('VIEW_ONLY_TEACHER');
