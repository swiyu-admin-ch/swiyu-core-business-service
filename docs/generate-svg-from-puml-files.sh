#!/usr/bin/env bash
#
# Generate SVG diagrams for all PlantUML `.puml` files in current folders and subfolders.
# The script scans recursively and writes each `.svg` next to its source file.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODEL_DIR="${SCRIPT_DIR}"

find "$MODEL_DIR" -type f -name "*.puml" | while IFS= read -r puml_file; do
  svg_file="${puml_file%.puml}.svg"
  rel_path="${svg_file#${MODEL_DIR}/}"
  echo "Generating: ${rel_path}"
  sed 's/@startuml[[:space:]].*/\@startuml/' "$puml_file" \
    | plantuml -tsvg -pipe \
    | sed 's|<svg\([^>]*\)>|<svg\1><rect width="100%" height="100%" fill="white"/>|' \
    > "$svg_file"
done
echo "Done."
