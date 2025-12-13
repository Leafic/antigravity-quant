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
                    new StockMaster("005930", "삼성전자", "KOSPI", "전기전자"),
                    new StockMaster("000660", "SK하이닉스", "KOSPI", "전기전자"),
                    new StockMaster("373220", "LG에너지솔루션", "KOSPI", "전기전자"),
                    new StockMaster("207940", "삼성바이오로직스", "KOSPI", "의약품"),
                    new StockMaster("005380", "현대차", "KOSPI", "운수장비"),
                    new StockMaster("000270", "기아", "KOSPI", "운수장비"),
                    new StockMaster("068270", "셀트리온", "KOSPI", "의약품"),
                    new StockMaster("005490", "POSCO홀딩스", "KOSPI", "철강금속"),
                    new StockMaster("035420", "NAVER", "KOSPI", "서비스업"),
                    new StockMaster("006400", "삼성SDI", "KOSPI", "전기전자"),
                    new StockMaster("051910", "LG화학", "KOSPI", "화학"),
                    new StockMaster("035720", "카카오", "KOSPI", "서비스업"),
                    new StockMaster("105560", "KB금융", "KOSPI", "금융업"),
                    new StockMaster("028260", "삼성물산", "KOSPI", "유통업"),
                    new StockMaster("012330", "현대모비스", "KOSPI", "운수장비"),
                    new StockMaster("055550", "신한지주", "KOSPI", "금융업"),
                    new StockMaster("003670", "포스코퓨처엠", "KOSPI", "화학"),
                    new StockMaster("032830", "삼성생명", "KOSPI", "보험"),
                    new StockMaster("003550", "LG", "KOSPI", "지주회사"),
                    new StockMaster("015760", "한국전력", "KOSPI", "전기가스업"),
                    new StockMaster("086790", "하나금융지주", "KOSPI", "금융업"),
                    new StockMaster("034730", "SK", "KOSPI", "지주회사"),
                    new StockMaster("034020", "두산에너빌리티", "KOSPI", "기계"),
                    new StockMaster("000810", "삼성화재", "KOSPI", "보험"),
                    new StockMaster("323410", "카카오뱅크", "KOSPI", "은행"),
                    new StockMaster("017670", "SK텔레콤", "KOSPI", "통신업"),
                    new StockMaster("018260", "삼성에스디에스", "KOSPI", "서비스업"),
                    new StockMaster("042660", "한화오션", "KOSPI", "운수장비"),
                    new StockMaster("033780", "KT&G", "KOSPI", "제조업"),
                    new StockMaster("009150", "삼성전기", "KOSPI", "전기전자"),

                    // KOSDAQ Top
                    new StockMaster("247540", "에코프로비엠", "KOSDAQ", "일반전기전자"),
                    new StockMaster("086520", "에코프로", "KOSDAQ", "금융"),
                    new StockMaster("091990", "셀트리온헬스케어", "KOSDAQ", "유통"),
                    new StockMaster("022100", "포스코DX", "KOSDAQ", "IT서비스"),
                    new StockMaster("066970", "엘앤에프", "KOSDAQ", "일반전기전자"),
                    new StockMaster("196170", "알테오젠", "KOSDAQ", "기타서비스"),
                    new StockMaster("035900", "JYP Ent.", "KOSDAQ", "오락문화"),
                    new StockMaster("278280", "천보", "KOSDAQ", "화학"),
                    new StockMaster("293490", "카카오게임즈", "KOSDAQ", "디지털컨텐츠"),
                    new StockMaster("041510", "에스엠", "KOSDAQ", "오락문화"),
                    new StockMaster("102940", "코오롱티슈진", "KOSDAQ", "기타서비스"),
                    new StockMaster("051900", "LG생활건강", "KOSPI", "화학"), // Added more for good measure
                    new StockMaster("010140", "삼성중공업", "KOSPI", "운수장비"),
                    new StockMaster("010950", "S-Oil", "KOSPI", "화학"),
                    new StockMaster("011070", "LG이노텍", "KOSPI", "전기전자"),
                    new StockMaster("009830", "한화솔루션", "KOSPI", "화학"),
                    new StockMaster("030200", "KT", "KOSPI", "통신업"),

                    // User Requested & KOSDAQ Biotech/Pharma
                    new StockMaster("314130", "지놈앤컴퍼니", "KOSDAQ", "제약"),
                    new StockMaster("006620", "동구바이오제약", "KOSDAQ", "제약"),
                    new StockMaster("348150", "고바이오랩", "KOSDAQ", "제약"),
                    new StockMaster("298540", "에이비엘바이오", "KOSDAQ", "제약"),
                    new StockMaster("028300", "HLB", "KOSDAQ", "제약"),
                    new StockMaster("067630", "HLB생명과학", "KOSDAQ", "제약"),
                    new StockMaster("000250", "삼천당제약", "KOSDAQ", "제약"),
                    new StockMaster("087010", "펩트론", "KOSDAQ", "제약"),
                    new StockMaster("214450", "파마리서치", "KOSDAQ", "제약"),
                    new StockMaster("237690", "에스티팜", "KOSDAQ", "제약"),
                    new StockMaster("145020", "휴젤", "KOSDAQ", "제약"),
                    new StockMaster("084990", "헬릭스미스", "KOSDAQ", "제약"),
                    new StockMaster("085660", "차바이오텍", "KOSDAQ", "제약"),
                    new StockMaster("214150", "클래시스", "KOSDAQ", "의료정밀"),
                    new StockMaster("039200", "오스코텍", "KOSDAQ", "제약"));
            repository.saveAll(seeds);
            log.info("Seeded {} stocks.", seeds.size());
        }
    }

    public List<StockMaster> search(String query) {
        return repository.search(query, PageRequest.of(0, 20));
    }
}
