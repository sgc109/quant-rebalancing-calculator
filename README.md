# quant-rebalancing-calculator

This code facilitates the easy rebalancing of portfolios following well-known dynamic asset allocation strategies such
as VAA(Vigilant Asset Allocation)([Paper](https://papers.ssrn.com/sol3/papers.cfm?abstract_id=3002624)) and HAA(Hybrid
Asset Allocation)([Paper](https://papers.ssrn.com/sol3/papers.cfm?abstract_id=4346906))

HAA is a highly potent dynamic asset allocation strategy, recording an impressive Compound Annual Growth Rate (CAGR) of
16.0% and a maximum drawdown (MDD) of -10.0% from 1970 to 2023. However, due to the need to calculate momentum scores
for a total of 10 ETFs, the process of manual calculation can be quite cumbersome. Similarly, VAA also
involves the calculation of 13612W scores for 14 different assets, adding to the complexity. This code addresses such
inconveniences by automating the calculations, providing guidance on how much to buy and sell for a specific portfolio
when rebalancing.

## How it works?

* Utilizing the Free Tier of the [Alpaca](https://alpaca.markets/) Market Data API, the program fetches stock prices
  with a 15-minute delay at most.
* The rebalancing results (date, additional investment amount, quantity of stocks per asset) are recorded in the
  stocks-history.yaml file at each rebalancing point. During program execution, it calculates and advises on the
  quantity to buy or sell for each asset based on the last recorded information, appending the results to the end of the
  file.
* Since all supported strategies involve U.S. ETFs, prices are denominated in USD.
* 모멘텀 스코어 계산 시 현재를 기점으로 가장 마지막 분봉이 생성된 시간을 현재로 간주하며, 정확히 1개월, 3개월, 6개월, 12개월 전 분봉의 종가를 기준으로 수익률을 계산합니다.

## Usage

### Preparation

* `src/main/resources` 내 alpaca.properties 파일을 생성합니다.
* alpaca.properties 에 다음과 같은 형식으로 Alpaca API Key, Secret Key 를 입력합니다.
    * [Alpaca 개발자 콘솔](https://app.alpaca.markets/paper/dashboard/overview)에서 API Key, Secret Key 를 발급받을 수 있습니다.

```
key_id = <KEY_ID>
secret_key = <SECRET_KEY>
endpoint_api_type = paper
data_api_type = iex
```

* (Optional) 만약 VAA/HAA 로 모두 전환하고 싶은 기존 포트폴리오가 있다면, `src/main/resources` 내 stocks-history.yaml 파일을 생성하여 아래와같은 형식으로 보유
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

### Practical use

* Execute Main.kt at the point where monthly rebalancing is required.
    * Modify the ADDITIONAL_MONEY_TO_DEPOSIT constant at the top of the file to the amount (in USD) you intend to invest
      additionally in this rebalancing.
    * Modify the MONEY_TO_WITHDRAW constant at the top of the file to the amount (in USD) you plan to withdraw in this
      rebalancing.
    * (Deprecated) <strike>Adjust the creation of the reBalancingHelper to instantiate the ReBalancingHelper object for
      the desired strategy.</strike> It will be supported soon.
* Refer to the output results or the section appended at the end of the stocks-history.yaml file and directly buy/sell
  stocks in your HTS(Home Trading System).

## Caution

* stocks-history.yaml 파일의 마지막에 현재 보유하고있는 각 주식의 수량을 정확하게 입력했는지 확인하세요.
    * 만약 잘못 입력되어있다면 리밸런싱 전 총 자산을 잘못 계산할 수 있고, 이는 결과적으로 매수/매도해야하는 주식의 수량을 잘못 계산하게 됩니다.
* 주당 가격이 높은 종목(e.g. SPY, QQQ, ...)이 포함된 경우 총 투자 금액이 너무 적은 경우 목표하는 비율대로 배분이 되지 않을 수 있기 때문에
  처음에 `ADDITIONAL_MONEY_TO_DEPOSIT`는 일정 수준 이상인것이 좋습니다(e.g. 10000 USD 이상)

## Future Development

* (WIP) Introduction of Kotlin Notebook for Easy Backtesting
    * Add a [Kotlin Notebook](https://kotlinlang.org/docs/data-science-overview.html#kotlin-notebook) to facilitate easy
      backtesting, leveraging the capabilities provided by Kotlin Notebook.
* Support for configuring trading fee and tax on rebalancing and backtesting.
* Support for Different Portfolios
    * Classic Portfolio, Permanent Portfolio, All Seasons, All Weather, GAA, PAA, DAA, LAA, BAA, ABAA, and more will be
      supported.
* Test Coverage
    * Write tests for key edge cases.
    * Utilize Property Testing to ensure that unexpected inputs do not lead to exceptions and thoroughly validate the
      code.
* Automated Rebalancing and Notification
    * Implement automatic rebalancing and historical record-keeping at predefined intervals using AWS Lambda + S3,
      Github Actions, or similar services.
    * Configure notifications via Telegram/LINE/KakaoTalk to receive alerts on successful or failed rebalancing.
* Incorporate Automated Trading Features
    * Integrate automatic trading functionalities to enable seamless execution of rebalancing strategies.
