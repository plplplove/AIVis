#!/bin/bash

# Script to clean up comments and logging from Kotlin files for production release
# Run from project root: ./cleanup_code.sh

echo "🧹 Starting code cleanup for production release..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
files_processed=0
comments_removed=0
logs_removed=0

# Backup directory
BACKUP_DIR="./code_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
echo "📦 Created backup directory: $BACKUP_DIR"
echo ""

# Find all Kotlin files
KOTLIN_FILES=$(find ./app/src/main/java/com/ai/vis -name "*.kt" -type f)

for file in $KOTLIN_FILES; do
    # Skip test files (optional - remove if you want to clean tests too)
    if [[ $file == *"/test/"* ]] || [[ $file == *"/androidTest/"* ]]; then
        continue
    fi
    
    # Create backup
    backup_path="$BACKUP_DIR/$(dirname "$file")"
    mkdir -p "$backup_path"
    cp "$file" "$backup_path/$(basename "$file")"
    
    # Create temp file
    temp_file="${file}.tmp"
    
    # Process file line by line
    in_multiline_comment=false
    file_modified=false
    
    while IFS= read -r line || [ -n "$line" ]; do
        original_line="$line"
        
        # Check for multiline comment start
        if [[ $line =~ ^[[:space:]]*/\*\* ]]; then
            in_multiline_comment=true
            comments_removed=$((comments_removed + 1))
            file_modified=true
            continue
        fi
        
        # Skip lines inside multiline comments
        if [ "$in_multiline_comment" = true ]; then
            if [[ $line =~ \*/ ]]; then
                in_multiline_comment=false
            fi
            continue
        fi
        
        # Remove single-line comments (entire line)
        if [[ $line =~ ^[[:space:]]*// ]]; then
            comments_removed=$((comments_removed + 1))
            file_modified=true
            continue
        fi
        
        # Remove inline comments (// at end of line) - preserve code before //
        if [[ $line =~ ^([^\"]*)(//[[:space:]].*)$ ]]; then
            # Make sure it's not inside a string
            code_part="${BASH_REMATCH[1]}"
            # Simple check: even number of quotes before // means it's outside string
            quote_count=$(echo "$code_part" | tr -cd '"' | wc -c)
            if [ $((quote_count % 2)) -eq 0 ]; then
                line="$code_part"
                comments_removed=$((comments_removed + 1))
                file_modified=true
            fi
        fi
        
        # Remove Log statements
        if [[ $line =~ Log\.[deiw] ]]; then
            logs_removed=$((logs_removed + 1))
            file_modified=true
            continue
        fi
        
        # Remove TAG constants used for logging
        if [[ $line =~ private[[:space:]]+const[[:space:]]+val[[:space:]]+TAG[[:space:]]*=[[:space:]]*\".*\" ]] || \
           [[ $line =~ const[[:space:]]+val[[:space:]]+TAG[[:space:]]*=[[:space:]]*\".*\" ]]; then
            logs_removed=$((logs_removed + 1))
            file_modified=true
            continue
        fi
        
        # Write line to temp file
        echo "$line" >> "$temp_file"
        
    done < "$file"
    
    # Replace original file if modified
    if [ "$file_modified" = true ]; then
        mv "$temp_file" "$file"
        files_processed=$((files_processed + 1))
        echo -e "${GREEN}✓${NC} Cleaned: ${file#./app/src/main/java/com/ai/vis/}"
    else
        rm -f "$temp_file"
    fi
done

echo ""
echo "════════════════════════════════════════"
echo -e "${GREEN}✅ Cleanup complete!${NC}"
echo "════════════════════════════════════════"
echo -e "Files processed:    ${YELLOW}$files_processed${NC}"
echo -e "Comments removed:   ${YELLOW}$comments_removed${NC}"
echo -e "Log statements:     ${YELLOW}$logs_removed${NC}"
echo ""
echo -e "📦 Backup saved to: ${YELLOW}$BACKUP_DIR${NC}"
echo ""
echo "⚠️  Next steps:"
echo "   1. Review changes with: git diff"
echo "   2. Test compilation"
echo "   3. If something broke, restore from backup"
echo "   4. Remove unused imports with IDE (Optimize Imports)"
echo ""
