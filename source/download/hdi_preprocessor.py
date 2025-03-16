import pandas as pd

# Read the CSV
df = pd.read_csv("human-development-index.csv", encoding='latin1')

# Generates hdi_1990, hdi_1991, ..., hdi_2022
year_columns = [f"hdi_{year}" for year in range(1990, 2023)]  

# Melt the year columns into rows
melted = df.melt(
    id_vars=["iso3", "country", "region"],  # Keep these as identifiers
    value_vars=[col for col in df.columns if col.startswith("hdi_")],  # Select hdi_1990 to hdi_2022
    var_name="year_column",
    value_name="hdiValue"
)

# Extract the year from "hdi_1990" -> "1990"
melted["year"] = melted["year_column"].str.replace("hdi_", "")

# Drop rows with missing hdiValue and the temporary year_column
melted = melted.dropna(subset=["hdiValue"]).drop(columns=["year_column"])

# Save to a new CSV
melted.to_csv("hdi_pivoted.csv", index=False)