-- Add new capability
INSERT INTO capabilities (name, description)
VALUES ('VIEW_USER', 'View user details');

-- Assign VIEW_USER capability to ADMIN role
INSERT INTO roles_capabilities (role_id, capability_id)
SELECT r.id, c.id
FROM roles r
JOIN capabilities c ON c.name = 'VIEW_USER'
WHERE r.name = 'ADMIN';