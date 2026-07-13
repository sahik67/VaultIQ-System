import os
import re

input_dir = r"E:/Settings/porn-sites"
output_file = r"E:/Settings/spy-app/app/src/main/assets/blocked_domains.txt"

domain_regex = re.compile(r"([a-z0-9\-]+\.[a-z0-9\.]+)")
unique_domains = set()

for filename in os.listdir(input_dir):
    if filename.endswith(".txt"):
        print(f"Processing {filename}...")
        try:
            with open(os.path.join(input_dir, filename), "r", encoding="utf-8", errors="ignore") as f:
                for line in f:
                    match = domain_regex.search(line.lower())
                    if match:
                        unique_domains.add(match.group(1))
        except Exception as e:
            print(f"Error reading {filename}: {e}")

print(f"Total unique domains found: {len(unique_domains)}")

os.makedirs(os.path.dirname(output_file), exist_ok=True)
with open(output_file, "w", encoding="utf-8") as f:
    for domain in sorted(unique_domains):
        f.write(domain + "\n")

print("Done!")
