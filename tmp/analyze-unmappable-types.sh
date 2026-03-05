#!/bin/bash
# Analyze unmappable types from the generation output

echo "=== Analysis of Unmappable Types ==="
echo ""

# Re-run generation and capture warnings
cd ponysdk
../gradlew generateWebAwesomeWrappers --no-daemon 2>&1 | grep "Unmappable type:" | sort | uniq -c | sort -rn > ../tmp/unmappable-types.txt

cd ..
cat tmp/unmappable-types.txt

echo ""
echo "=== Categories ==="
echo ""

echo "DOM Element Types:"
grep -E "HTML|Element|Window|Document" tmp/unmappable-types.txt | wc -l

echo ""
echo "Union Types with null/undefined:"
grep -E "\| null|\| undefined" tmp/unmappable-types.txt | wc -l

echo ""
echo "Array Types:"
grep "\[\]" tmp/unmappable-types.txt | wc -l

echo ""
echo "Complex Generic Types:"
grep -E "Map<|Set<|ReturnType<" tmp/unmappable-types.txt | wc -l

echo ""
echo "Empty/null type strings:"
grep "empty or null" tmp/unmappable-types.txt | wc -l

echo ""
echo "Function Types:"
grep "=>" tmp/unmappable-types.txt | wc -l

echo ""
echo "Custom Web Awesome Types:"
grep -E "Wa[A-Z]" tmp/unmappable-types.txt | wc -l

echo ""
echo "Other:"
grep -v -E "HTML|Element|Window|Document|\| null|\| undefined|\[\]|Map<|Set<|ReturnType<|empty or null|=>|Wa[A-Z]" tmp/unmappable-types.txt | wc -l
