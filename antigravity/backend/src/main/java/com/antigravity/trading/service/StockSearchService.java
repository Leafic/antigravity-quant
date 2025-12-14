package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.StockMaster;
import com.antigravity.trading.repository.StockMasterRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockSearchService {

    private final StockMasterRepository repository;

    @PostConstruct
    public void seedData() {
        if (repository.count() == 0) {
            log.info("Seeding StockMaster data...");
            List<StockMaster> seeds = Arrays.asList(
                    // KOSPI Top
                    createStock("005930", "삼성전자", "KOSPI", "전기전자"),
                    createStock("000660", "SK하이닉스", "KOSPI", "전기전자"),
                    createStock("373220", "LG에너지솔루션", "KOSPI", "전기전자"),
                    createStock("207940", "삼성바이오로직스", "KOSPI", "의약품"),
                    createStock("005380", "현대차", "KOSPI", "운수장비"),
                    createStock("000270", "기아", "KOSPI", "운수장비"),
                    createStock("068270", "셀트리온", "KOSPI", "의약품"),
                    createStock("005490", "POSCO홀딩스", "KOSPI", "철강금속"),
                    createStock("035420", "NAVER", "KOSPI", "서비스업"),
                    createStock("006400", "삼성SDI", "KOSPI", "전기전자"),
                    createStock("051910", "LG화학", "KOSPI", "화학"),
                    createStock("035720", "카카오", "KOSPI", "서비스업"),
                    createStock("105560", "KB금융", "KOSPI", "금융업"),
                    createStock("028260", "삼성물산", "KOSPI", "유통업"),
                    createStock("012330", "현대모비스", "KOSPI", "운수장비"),
                    createStock("055550", "신한지주", "KOSPI", "금융업"),
                    createStock("003670", "포스코퓨처엠", "KOSPI", "화학"),
                    createStock("032830", "삼성생명", "KOSPI", "보험"),
                    createStock("003550", "LG", "KOSPI", "지주회사"),
                    createStock("015760", "한국전력", "KOSPI", "전기가스업"),
                    createStock("086790", "하나금융지주", "KOSPI", "금융업"),
                    createStock("034730", "SK", "KOSPI", "지주회사"),
                    createStock("034020", "두산에너빌리티", "KOSPI", "기계"),
                    createStock("000810", "삼성화재", "KOSPI", "보험"),
                    createStock("323410", "카카오뱅크", "KOSPI", "은행"),
                    createStock("017670", "SK텔레콤", "KOSPI", "통신업"),
                    createStock("018260", "삼성에스디에스", "KOSPI", "서비스업"),
                    createStock("042660", "한화오션", "KOSPI", "운수장비"),
                    createStock("033780", "KT&G", "KOSPI", "제조업"),
                    createStock("009150", "삼성전기", "KOSPI", "전기전자"),

                    // KOSDAQ Top
                    createStock("247540", "에코프로비엠", "KOSDAQ", "일반전기전자"),
                    createStock("086520", "에코프로", "KOSDAQ", "금융"),
                    createStock("091990", "셀트리온헬스케어", "KOSDAQ", "유통"),
                    createStock("022100", "포스코DX", "KOSDAQ", "IT서비스"),
                    createStock("066970", "엘앤에프", "KOSDAQ", "일반전기전자"),
                    createStock("196170", "알테오젠", "KOSDAQ", "기타서비스"),
                    createStock("035900", "JYP Ent.", "KOSDAQ", "오락문화"),
                    createStock("278280", "천보", "KOSDAQ", "화학"),
                    createStock("293490", "카카오게임즈", "KOSDAQ", "디지털컨텐츠"),
                    createStock("041510", "에스엠", "KOSDAQ", "오락문화"),
                    createStock("102940", "코오롱티슈진", "KOSDAQ", "기타서비스"),
                    createStock("051900", "LG생활건강", "KOSPI", "화학"),
                    createStock("010140", "삼성중공업", "KOSPI", "운수장비"),
                    createStock("010950", "S-Oil", "KOSPI", "화학"),
                    createStock("011070", "LG이노텍", "KOSPI", "전기전자"),
                    createStock("009830", "한화솔루션", "KOSPI", "화학"),
                    createStock("030200", "KT", "KOSPI", "통신업"),

                    // User Requested & KOSDAQ Biotech/Pharma
                    createStock("314130", "지놈앤컴퍼니", "KOSDAQ", "제약"),
                    createStock("006620", "동구바이오제약", "KOSDAQ", "제약"),
                    createStock("348150", "고바이오랩", "KOSDAQ", "제약"),
                    createStock("298540", "에이비엘바이오", "KOSDAQ", "제약"),
                    createStock("028300", "HLB", "KOSDAQ", "제약"),
                    createStock("067630", "HLB생명과학", "KOSDAQ", "제약"),
                    createStock("000250", "삼천당제약", "KOSDAQ", "제약"),
                    createStock("087010", "펩트론", "KOSDAQ", "제약"),
                    createStock("214450", "파마리서치", "KOSDAQ", "제약"),
                    createStock("237690", "에스티팜", "KOSDAQ", "제약"),
                    createStock("145020", "휴젤", "KOSDAQ", "제약"),
                    createStock("084990", "헬릭스미스", "KOSDAQ", "제약"),
                    createStock("085660", "차바이오텍", "KOSDAQ", "제약"),
                    createStock("214150", "클래시스", "KOSDAQ", "의료정밀"),
                    createStock("039200", "오스코텍", "KOSDAQ", "제약"));
            repository.saveAll(seeds);
            log.info("Seeded {} stocks.", seeds.size());
        }
    }

    private StockMaster createStock(String code, String name, String market, String sector) {
        return StockMaster.builder()
                .code(code)
                .name(name)
                .market(market)
                .sector(sector)
                .isFavorite(false)
                .isManaged(false)
                .isSuspended(false)
                .lastUpdated(java.time.LocalDateTime.now())
                .build();
    }

    public List<StockMaster> search(String query) {
        return repository.search(query, PageRequest.of(0, 20));
    }
}
