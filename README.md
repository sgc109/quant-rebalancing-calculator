# quant-rebalancing-calculator

잘 알려진 동적 자산배분 전략인 VAA(Vigilant Asset Allocation)([Paper](https://papers.ssrn.com/sol3/papers.cfm?abstract_id=3002624)), HAA(Hybrid Asset Allocation)([Paper](https://papers.ssrn.com/sol3/papers.cfm?abstract_id=4346906))
포트폴리오의
리밸런싱을 쉽게 할 수 있게 도와주는 코드입니다.

VAA는 1973년부터 2021년까지 12.6% 의 연 복리 수익률(CAGR)과 -10.2% 의 최대 손실률(MDD)을 기록하는 매우 강력한 동적 자산배분 전략이지만(VAA-G12 기준), 총 14가지 ETF 의
13612W 모멘텀 스코어를 계산해야 하기 때문에 매번 직접 계산하기가 매우 번거롭습니다. HAA 는 10가지 자산의 13612U 스코어를 계산해야해서 번거롭기는 마찬가지입니다.
본 코드는 이러한 불편함을 해소하기 위해 계산을 자동화하여 특정 포트폴리오를 기준으로 리밸런싱 시 어떤 자산을 얼만큼 사고 팔아야하는지 알려줍니다.

## 동작 방식

* [Alpaca](https://alpaca.markets/) 의 Market Data API 의 Free Tier 를 사용하여 15분 지연된 주가 정보를 가져옵니다.
* stocks-history.yaml 에 리밸런싱하는 시점마다 리밸런싱 결과(날짜, 추가 투자 금액, 종목 별 주식 수량)를 기록하며, 프로그램 실행 시 마지막 기록을 기반으로 계산하여 어떤 종목을 얼마나
  사야하는지, 혹은
  팔아야하는지 알려주고 결과를 파일 끝에 기록합니다.
* 지원하는 모든 전략들의 종목은 모두 미국 ETF 이기 때문에 모든 가격은 USD 를 기준으로 합니다.
* 모멘텀 스코어 계산 시 현재를 기점으로 가장 마지막 분봉이 생성된 시간을 현재로 간주하며, 정확히 1개월, 3개월, 6개월, 12개월 전 분봉의 종가를 기준으로 수익률을 계산합니다.

## 사용법

### 사전 세팅

* `src/main/resources` 내 alpaca.properties 파일을 생성합니다.
* alpaca.properties 에 다음과 같은 형식으로 Alpaca API Key, Secret Key 를 입력합니다.
    * [Alpaca 개발자 콘솔](https://app.alpaca.markets/paper/dashboard/overview)에서 API Key, Secret Key 를 발급받을 수 있습니다.

```
key_id = <KEY_ID>
secret_key = <SECRET_KEY>
endpoint_api_type = paper
data_api_type = iex
```

* (Optional) 만약 VVA-Balanced 로 모두 전환하고 싶은 포트폴리오가 있다면, `src/main/resources` 내 stocks-history.yaml 파일을 생성하여 아래와같은 형식으로 보유
  주식들에 대한
  수량을 입력합니다.

```
date: 2023-12-28
stocks:
  SPY: 1
  QQQ: 7
  IWM: 9
  VGK: 0
  EWJ: 2
  EEM: 7
  VNQ: 10
  GLD: 13
  DBC: 19
  HYG: 6
  LQD: 5
  TLT: 3
```

### 실전 사용

* 매달 1회 리밸런싱을 해야하는 시점에 Main.kt 를 실행합니다.
    * 파일 최상단에 있는 `ADDITIONAL_MONEY_TO_DEPOSIT` 상수를 내가 이번 리밸런싱에 추가로 투자할 금액(USD 기준)으로 수정합니다.
    * 파일 최상단에 있는 `MONEY_TO_WITHDRAW` 상수를 내가 이번 리밸런싱에 회수할 금액(USD 기준)으로 수정합니다.
    * `reBalancingHelper`를 생성하는 부분에서 원하는 전략의 ReBalancingHelper 객체를 생성하도록 수정합니다.
* 출력 결과 또는 stocks-history.yaml 파일 마지막에 추가된 부분을 참고하여 직접 HTS 에서 주식을 매수/매도합니다.

## 주의사항

* stocks-history.yaml 파일의 마지막에 현재 보유하고있는 각 주식의 수량을 정확하게 입력했는지 확인하세요.
    * 만약 잘못 입력되어있다면 리밸런싱 전 총 자산을 잘못 계산할 수 있고, 이는 결과적으로 매수/매도해야하는 주식의 수량을 잘못 계산하게 됩니다.
* 주당 가격이 높은 종목(e.g. SPY, QQQ, ...)이 포함된 경우 총 투자 금액이 너무 적은 경우 목표하는 비율대로 배분이 되지 않을 수 있기 때문에
  처음에 `ADDITIONAL_MONEY_TO_DEPOSIT`는 일정 수준 이상인것이 좋습니다(e.g. 10000 USD 이상)

## 향후 개발 방향성

* 다른 포트폴리오도 지원
    * (e.g. 60/40, Permanent Portfolio, 올시즌, 올웨더, GAA, PAA, DAA, LAA, BAA, ABAA, ...)
* 리팩터링(여러가지 포트폴리오를 쉽게 요리조리 조립할 수 있도록 공통 코드를 분리하고 class hierarchy 를 잘 구성)
* 테스트 작성
  * 주요 엣지 케이스에 대해 테스트 작성
  * 코드를 확실하게 검증하기 위해 Property Testing 으로 예상하지 못한 입력에 대해 예외가 발생하지는 않는지 테스트
* 자동매매 기능 추가
* AWS Lambda, S3 등을 통해 정해진 주기마다 자동 리밸런싱 및 히스토리 기록 & 카톡으로 성공/실패 알림
* 손쉽게 백트래킹을 할 수 있는 [Kotlin Notebook](https://kotlinlang.org/docs/data-science-overview.html#kotlin-notebook) 추가
