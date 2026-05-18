const PLACE_LIST = [
  "강남 MICE 관광특구",
  "동대문 관광특구",
  "명동 관광특구",
  "이태원 관광특구",
  "잠실 관광특구",
  "종로·청계 관광특구",
  "홍대 관광특구",
  "경복궁",
  "광화문·덕수궁",
  "보신각",
  "서울 암사동 유적",
  "창덕궁·종묘",
  "가산디지털단지역",
  "강남역",
  "건대입구역",
  "고덕역",
  "고속터미널역",
  "교대역",
  "구로디지털단지역",
  "구로역",
  "군자역",
  "대림역",
  "동대문역",
  "뚝섬역",
  "미아사거리역",
  "발산역",
  "사당역",
  "삼각지역",
  "서울대입구역",
  "서울식물원·마곡나루역",
  "서울역",
  "선릉역",
  "성신여대입구역",
  "수유역",
  "신논현역·논현역",
  "신도림역",
  "신림역",
  "신촌·이대역",
  "양재역",
  "역삼역",
  "연신내역",
  "오목교역·목동운동장",
  "왕십리역",
  "용산역",
  "이태원역",
  "장지역",
  "장한평역",
  "천호역",
  "총신대입구(이수)역",
  "충정로역",
  "합정역",
  "혜화역",
  "홍대입구역(2호선)",
  "회기역",
  "가락시장",
  "가로수길",
  "광장(전통)시장",
  "김포공항",
  "노량진",
  "덕수궁길·정동길",
  "북촌한옥마을",
  "서촌",
  "성수카페거리",
  "쌍문역",
  "압구정로데오거리",
  "여의도",
  "연남동",
  "영등포 타임스퀘어",
  "용리단길",
  "이태원 앤틱가구거리",
  "인사동",
  "창동 신경제 중심지",
  "청담동 명품거리",
  "청량리 제기동 일대 전통시장",
  "해방촌·경리단길",
  "DDP(동대문디자인플라자)",
  "DMC(디지털미디어시티)",
  "강서한강공원",
  "고척돔",
  "광나루한강공원",
  "광화문광장",
  "국립중앙박물관·용산가족공원",
  "난지한강공원",
  "남산공원",
  "노들섬",
  "뚝섬한강공원",
  "망원한강공원",
  "반포한강공원",
  "북서울꿈의숲",
  "서리풀공원·몽마르뜨공원",
  "서울대공원",
  "서울숲공원",
  "아차산",
  "양화한강공원",
  "어린이대공원",
  "여의도한강공원",
  "월드컵공원",
  "응봉산",
  "이촌한강공원",
  "잠실종합운동장",
  "잠실한강공원",
  "잠원한강공원",
  "청계산",
  "북창동 먹자골목",
  "남대문시장",
  "익선동",
  "신정네거리역",
  "잠실새내역",
  "잠실역",
  "잠실롯데타워·석촌호수",
  "송리단길·호수단길",
  "신촌 스타광장",
  "보라매공원",
  "서대문독립공원",
  "안양천",
  "여의서로",
  "올림픽공원",
  "홍제폭포",
  "송현녹지광장",
  "시의회 앞",
  "숭례문"
];

export default {
  // 1. 안드로이드 요청 처리
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const headers = {
      "Content-Type": "application/json;charset=UTF-8",
      "Access-Control-Allow-Origin": "*",
    };

    if (url.pathname === "/api/all-places") {
      const cachedDataStr = await env.SEOUL_CACHE.get("all_places_data");
      if (!cachedDataStr) {
        return new Response(JSON.stringify({ error: "데이터 수집 중입니다." }), { status: 503, headers });
      }
      return new Response(cachedDataStr, { status: 200, headers });
    }
    return new Response("Seoul Outing Proxy is Running!", { status: 200 });
  },

  // 2. 순환 동기화 스케줄러 (무료 50회 제한 회피)
  async scheduled(event, env, ctx) {
    const apiKey = env.SEOUL_API_KEY;

    // 현재 실행할 그룹 번호 가져오기 (0, 1, 2)
    let currentGroup = parseInt(await env.SEOUL_CACHE.get("sync_group") || "0");

    // 121곳을 45곳씩 자르기 (50회 제한 아래로)
    const chunkSize = 45;
    const startIndex = currentGroup * chunkSize;
    const endIndex = startIndex + chunkSize;
    const targetPlaces = PLACE_LIST.slice(startIndex, endIndex);

    if (targetPlaces.length === 0) return;

    // 45곳 데이터 실시간 조회 (안정성을 위해 10개씩 묶어서)
    const results = [];
    for (let i = 0; i < targetPlaces.length; i += 10) {
      const chunk = targetPlaces.slice(i, i + 10);
      const fetchPromises = chunk.map(async (place) => {
        const apiUrl = `http://openapi.seoul.go.kr:8088/${apiKey}/json/citydata/1/5/${encodeURIComponent(place)}`;
        try {
          const res = await fetch(apiUrl);
          if (!res.ok) return null;
          const data = await res.json();
          const cityData = data.CITYDATA;
          if (!cityData) return null;

          const weather = cityData.WEATHER_STTS?.[0] || {};
          const livePpltn = cityData.LIVE_PPLTN_STTS?.[0] || {};
          
          const events = cityData.EVENT_STTS || [];
          const eventValue = events.length > 0 ? 1.0 : 0.0;
          const eventName = events
            .map(e => e.EVENT_NM)          
            .filter(name => name)           
            .join(", ");
          
          return {
            placeName: place,
            temp: parseFloat(weather.TEMP || 0),
            pmIndex: parseFloat(weather.PM10 || 0),
            congestion: livePpltn.AREA_CONGEST_LVL || "보통",
            localEvent: eventValue,  
            eventName: eventName     
          };
        } catch (err) {
          return null;
        }
      });

      const chunkResults = await Promise.all(fetchPromises);
      chunkResults.forEach(item => { if (item) results.push(item); });
      await new Promise(resolve => setTimeout(resolve, 500));
    }

    // KV에 저장된 전체 데이터(121곳) 불러오기
    let existingData = [];
    const cachedStr = await env.SEOUL_CACHE.get("all_places_data");
    if (cachedStr) {
      existingData = JSON.parse(cachedStr);
    }

    // 기존 데이터에서 '이번에 새로 가져온 장소'들만 최신 데이터로 덮어쓰기
    let updatedData = existingData.filter(oldItem => !targetPlaces.includes(oldItem.placeName));
    updatedData.push(...results);

    // 최종 병합된 데이터(최대 121곳) 다시 저장
    await env.SEOUL_CACHE.put("all_places_data", JSON.stringify(updatedData));

    // 다음 그룹 번호 지정 (0 -> 1 -> 2 -> 0 무한 반복)
    let nextGroup = currentGroup + 1;
    if (nextGroup * chunkSize >= PLACE_LIST.length) {
      nextGroup = 0; // 끝까지 돌았으면 처음으로 리셋
    }
    await env.SEOUL_CACHE.put("sync_group", nextGroup.toString());
  }
};
