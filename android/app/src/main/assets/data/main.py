from pprint import pprint
from textwrap import indent
from bs4 import BeautifulSoup
import requests
import json

# Fetch the list of T-Dolls from the gamepress website by scrapping for the table that contains them.
class TDollScraper:
    soup = None
    url = "https://gamepress.gg/girlsfrontline/t-dolls-list"

    def _fetch_table_rows(self):
        # Fetch the table rows from the table tag.
        table = self.soup.find("table", id = "t-doll-new-list")
        return table.find_all("tr", class_ = "t-doll-new-row")

    def _sort_key(self, e):
        # Sort by the T-Doll's ID.
        number = e.find("td", class_ = "id-cell").text.strip()
        if number == "":
            return 9999
        else:
            return int(number)

    def start(self):
        object_to_save = []

        res = requests.get(self.url)
        if res.ok:
            self.soup = BeautifulSoup(res.text, "html.parser")

            # Fetch the table rows that contain each T-Doll.
            table_rows = self._fetch_table_rows()
            print(len(table_rows))

            # Sort the table rows by the integer version of the T-Doll's ID.
            table_rows.sort(key=lambda e: self._sort_key(e))

            # Iterate through each T-Doll and save each into the list as an object.
            for tdoll in table_rows:
                id = tdoll.find("td", class_ = "id-cell").text.strip()
                name = tdoll.find("td", class_ = "title-cell").find("div").text.strip()
                type = tdoll.find("td", class_ = "class-cell").text.strip()
                rarity = tdoll.find("td", class_ = "rarity-cell").find("div").text.strip()
                # print(id + " " + name + " " + rarity)

                object_to_save.append({
                    "id": id,
                    "name": name,
                    "type": type,
                    "rarity": rarity
                })

            pprint(object_to_save)

            jsonString = json.dumps(object_to_save, indent = 4)
            with open("tdolls.json", "w") as file:
                file.write(jsonString)
        else:
            print(f"HTTP Request came back with {res.status_code}")

if __name__ == "__main__":
    scrapper = TDollScraper()
    scrapper.start()
