#!/bin/bash

# PostgreSQL backup script
BACKUP_DIR="backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/slackcat-prod_${TIMESTAMP}.sql"
KEEP_COUNT=3
CONTAINER_NAME="postgres_container"
DB_NAME="slackcatdb"
DB_USER="newuser"

# Create backup directory if it doesn't exist
mkdir -p "${BACKUP_DIR}"

# Create backup using pg_dump from the postgres container
echo "Creating backup: ${BACKUP_FILE}"
docker exec -t ${CONTAINER_NAME} pg_dump -U ${DB_USER} ${DB_NAME} > "${BACKUP_FILE}"

if [ $? -eq 0 ]; then
    echo "Backup created successfully"

    # Keep only the latest KEEP_COUNT backups
    BACKUP_COUNT=$(ls -1 "${BACKUP_DIR}"/slackcat-prod_*.sql 2>/dev/null | wc -l)

    if [ ${BACKUP_COUNT} -gt ${KEEP_COUNT} ]; then
        echo "Removing old backups (keeping ${KEEP_COUNT} most recent)"
        ls -1t "${BACKUP_DIR}"/slackcat-prod_*.sql | tail -n +$((KEEP_COUNT + 1)) | xargs rm -f
        echo "Cleanup complete"
    fi

    echo "Current backups:"
    ls -lht "${BACKUP_DIR}"/slackcat-prod_*.sql
else
    echo "Backup failed!"
    exit 1
fi
