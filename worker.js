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
  // 📥 1. 안드로이드 앱에서 실시간 데이터를 요청할 때 호출되는 API (GET /api/all-places)
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    
    // 안드로이드 통신 시 에러 방지 및 한글 깨짐 방지를 위한 헤더 설정
    const headers = {
      "Content-Type": "application/json;charset=UTF-8",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers });
    }

    if (url.pathname === "/health") {
      return new Response(JSON.stringify({ ok: true }), { status: 200, headers });
    }

    if (url.pathname === "/api/all-places") {
      // KV 저장소(SEOUL_CACHE)에서 스케줄러가 미리 모아둔 121곳 데이터를 즉시 꺼내옴
      const cachedDataStr = await env.SEOUL_CACHE.get("all_places_data");
      
      if (!cachedDataStr) {
        return new Response(
          JSON.stringify({ error: "서버에서 데이터를 수집 중입니다. 잠시 후 다시 시도해주세요." }), 
          { status: 503, headers }
        );
      }

      return new Response(cachedDataStr, { status: 200, headers });
    }

    return new Response("Seoul Outing Proxy is Running successfully!", { status: 200 });
  },

  // 🔄 2. 10분마다 자동으로 서울시 서버를 찌르는 백그라운드 스케줄러 (Cron Trigger)
  async scheduled(event, env, ctx) {
    console.log("🔄 스케줄러 구동: 서울시 실시간 데이터 121곳 전체 갱신 시작");
    const apiKey = env.SEOUL_API_KEY;
    const results = [];

    // 서울시 API 서버 과부하 및 IP 차단을 막기 위해 10개씩 묶어서(Chunk) 안전하게 호출
    const chunkSize = 10;
    for (let i = 0; i < PLACE_LIST.length; i += chunkSize) {
      const chunk = PLACE_LIST.slice(i, i + chunkSize);
      
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
          
          // 🚩 실시간 이벤트 정보 추출
          const events = cityData.EVENT_STTS || [];
          // AI 모델에 들어갈 실수형 피처 (축제가 있으면 1.0, 없으면 0.0)
          const eventValue = events.length > 0 ? 1.0 : 0.0;
          // UI 화면 카드뷰에 띄워줄 대표 축제 이름 (없으면 빈 문자열)
          const eventName = events.length > 0 ? (events[0].EVENT_NM || "") : "";

          return {
            placeName: place,
            temp: parseFloat(weather.TEMP || 0),
            pmIndex: parseFloat(weather.PM10 || 0),
            congestion: livePpltn.AREA_CONGEST_LVL || "보통",
            localEvent: eventValue,  // AI 입력용 (float)
            eventName: eventName     // ⬅️ 안드로이드 UI 출력용 (String)
          };
        } catch (err) {
          console.error(`[${place}] 데이터 조회 실패:`, err);
          return null;
        }
      });

      // 10개 병렬 처리 대기
      const chunkResults = await Promise.all(fetchPromises);
      chunkResults.forEach((item) => {
        if (item) results.push(item);
      });

      // 청크 사이에 0.5초(500ms) 휴식을 주어 통신 안정성 극대화
      await new Promise(resolve => setTimeout(resolve, 500));
    }

    // 수집 성공한 배열을 JSON 문자열로 변환하여 KV에 최종 저장
    if (results.length > 0) {
      await env.SEOUL_CACHE.put("all_places_data", JSON.stringify(results));
      console.log(`✅ 데이터 적재 완료! 총 ${results.length}곳 최신화 성공.`);
    }
  }
};
