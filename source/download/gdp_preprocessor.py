import pandas as pd
# Read the CSV
df = pd.read_csv("GDP.MKTP.CD_DS2_en.csv",  encoding='latin1')

# Remove characters from Country Name column
df['ï»¿"Country Name"'] = df['ï»¿"Country Name"'].str.replace('ï»¿"','').str.replace('"','')
df = df.rename(columns={'ï»¿"Country Name"': 'Country Name'})

# Generates "1960", "1961", ..., "2023"
year_columns = [str(year) for year in range(1960, 2024)] 
# Melt the year columns into rows
melted = df.melt(
    id_vars=["Country Name", "Country Code", "Indicator Name", "Indicator Code"],  # Keep these as identifiers
    value_vars=[col for col in year_columns if col in df.columns],  # Select only year columns present in the CSV
    var_name="year",
    value_name="gdpValue"
)

melted = melted.dropna(subset=["gdpValue"])

melted.to_csv("gdp_pivoted.csv", index=False)