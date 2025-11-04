# 일반/행사/모집 크롤링 (학교 페이지 상에서는 페이지가 분류되어 있지만, 사실상 같은 데이터 베이스 내에서 가져오는 것으로 보임. 이에 따라 크롤링 전략 고려 필요)
import requests
from bs4 import BeautifulSoup

page_num = str(2611)

# url은 같음. 하지만 데이터베이스에서 가져오는 것이기 때문에, 특정 페이지만 가져와도 좋음.
url = "https://www.inu.ac.kr/bbs/inu/" + page_num + "/artclList.do"

# 게시판 목록을 가져오기 위한 POST 파라미터
payload = {
    "layout": "Q0UK5PJj2ZsQo0aX2Frag0Mw8X0X3D",  # 이건 페이지 열었을 때마다 바뀔 수도 있음
    "menuNo": page_num,  # 메뉴 번호
    "page": "1",  # 가져올 페이지 번호
}

headers = {
    "User-Agent": "Mozilla/5.0",
}

response = requests.post(url, data=payload, headers=headers)
response.raise_for_status()

soup = BeautifulSoup(response.text, "html.parser")

# 게시글 목록은 아래 구조에 있음
rows = soup.select("table.board-table tbody tr")

result = []

for row in rows:
    a = row.select_one("a")
    title = a.get_text(strip=True)
    href = a["href"]
    full_url = "https://www.inu.ac.kr" + href

    writer = row.select_one(
        "td.td-write"
    ).text.strip()  # 파일 내에 입력된 값이 다름. 이에 따라, 파일 내에 있는 내용을 크롤링 하는 방식도 고려 필요
    date = row.select_one("td.td-date").text.strip()
    hits = row.select_one("td.td-access").text.strip()
    category = row.select_one("td.td-category").text.strip()
    result.append(
        {
            "title": title,
            "writer": writer,
            "date": date,
            "hits": hits,
            "url": full_url,
            "category": category,
        }
    )
for item in result:
    for name, content in item.items():
        print(f"{name} : {content}")
