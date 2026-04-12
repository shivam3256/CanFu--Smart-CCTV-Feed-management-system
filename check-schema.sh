#!/bin/bash
# Check database schema

echo "=== MySQL Schema Check ==="
echo ""

# Check if database exists
mysql -h localhost -u root -pShivam@9797 -e "SHOW DATABASES LIKE 'camfu_db';" 2>&1

echo ""
echo "=== Cameras Table Structure ==="
mysql -h localhost -u root -pShivam@9797 camfu_db -e "DESC cameras;" 2>&1

echo ""
echo "=== Cameras Table Data ==="
mysql -h localhost -u root -pShivam@9797 camfu_db -e "SELECT * FROM cameras LIMIT 5;" 2>&1
