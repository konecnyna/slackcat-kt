#!/bin/bash

DB_FILE="slackcat-prod.db"
BACKUP_DIR="backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/slackcat-prod_${TIMESTAMP}.db"
KEEP_COUNT=3

# Create backup directory if it doesn't exist
mkdir -p "${BACKUP_DIR}"

# Create backup
echo "Creating backup: ${BACKUP_FILE}"
cp "${DB_FILE}" "${BACKUP_FILE}"

if [ $? -eq 0 ]; then
    echo "Backup created successfully"

    # Keep only the latest KEEP_COUNT backups
    BACKUP_COUNT=$(ls -1 "${BACKUP_DIR}"/slackcat-prod_*.db 2>/dev/null | wc -l)

    if [ ${BACKUP_COUNT} -gt ${KEEP_COUNT} ]; then
        echo "Removing old backups (keeping ${KEEP_COUNT} most recent)"
        ls -1t "${BACKUP_DIR}"/slackcat-prod_*.db | tail -n +$((KEEP_COUNT + 1)) | xargs rm -f
        echo "Cleanup complete"
    fi

    echo "Current backups:"
    ls -lht "${BACKUP_DIR}"/slackcat-prod_*.db
else
    echo "Backup failed!"
    exit 1
fi
