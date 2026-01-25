# Changelog

## [0.1.1](https://github.com/hermanngeorge15/qawave-automation/compare/v0.1.0...v0.1.1) (2026-01-25)


### Features

* **backend:** add API endpoint integration tests ([153fefa](https://github.com/hermanngeorge15/qawave-automation/commit/153fefa4985a9cb6660682ffb57036b1713cb9ad)), closes [#35](https://github.com/hermanngeorge15/qawave-automation/issues/35)
* **backend:** add ktlint and detekt code quality tools ([97b04fd](https://github.com/hermanngeorge15/qawave-automation/commit/97b04fdfdc72cbe25230216be5798b52c17ff6b9)), closes [#79](https://github.com/hermanngeorge15/qawave-automation/issues/79)
* **backend:** add pagination support to R2DBC repositories ([2492043](https://github.com/hermanngeorge15/qawave-automation/commit/249204381f908f2ab556a93bf17cd59075b9c24d)), closes [#14](https://github.com/hermanngeorge15/qawave-automation/issues/14)
* **backend:** add Resilience4j circuit breaker for AI calls ([c4333dd](https://github.com/hermanngeorge15/qawave-automation/commit/c4333ddae14417b72d8fab2e0fabde33dcc7b876))
* **backend:** configure Spring Kafka for event streaming ([0e2558c](https://github.com/hermanngeorge15/qawave-automation/commit/0e2558cf49cbb613f1c53438cac104a04b9956ae)), closes [#30](https://github.com/hermanngeorge15/qawave-automation/issues/30)
* **backend:** implement AI client abstraction with OpenAI and stub providers ([16a634e](https://github.com/hermanngeorge15/qawave-automation/commit/16a634ed4e09c2dcbf53759bda51decc0e581de0)), closes [#16](https://github.com/hermanngeorge15/qawave-automation/issues/16)
* **backend:** implement ScenarioService for test scenario management ([5a00a22](https://github.com/hermanngeorge15/qawave-automation/commit/5a00a22ad805b79c6592caf1e44cd6cd0dde9233)), closes [#83](https://github.com/hermanngeorge15/qawave-automation/issues/83)
* **backend:** implement TestExecutionService for running test scenarios ([8742210](https://github.com/hermanngeorge15/qawave-automation/commit/87422102b2139d177cbec2d6a01971f2a203e1d4)), closes [#84](https://github.com/hermanngeorge15/qawave-automation/issues/84)
* **ci:** Skip Build & Push for pull requests ([cd2e9f2](https://github.com/hermanngeorge15/qawave-automation/commit/cd2e9f2c802a0accd7aba574a7712c1419a9295d)), closes [#81](https://github.com/hermanngeorge15/qawave-automation/issues/81)
* **e2e:** Set up Playwright E2E testing framework ([912424b](https://github.com/hermanngeorge15/qawave-automation/commit/912424b2886af6c94c1387b6255b228f0d90761c)), closes [#22](https://github.com/hermanngeorge15/qawave-automation/issues/22)
* Initialize QAWave multi-agent platform structure ([aa3fdd4](https://github.com/hermanngeorge15/qawave-automation/commit/aa3fdd495090700bb356feebc7be3d8ad57c3179))


### Bug Fixes

* **ci:** Add Kafka service container for integration tests ([0c1050c](https://github.com/hermanngeorge15/qawave-automation/commit/0c1050c305ef21d5b21ad281772c8a1ad1192a46)), closes [#81](https://github.com/hermanngeorge15/qawave-automation/issues/81)
* **ci:** Fix Docker build context and gradle wrapper ([15a0eab](https://github.com/hermanngeorge15/qawave-automation/commit/15a0eab3d4bc71eea07ffd0d0be4bef69b07d613)), closes [#81](https://github.com/hermanngeorge15/qawave-automation/issues/81)
* **ci:** Fix gradle working directory for all jobs ([9444000](https://github.com/hermanngeorge15/qawave-automation/commit/94440009f0bc4296511a52f58ed0415686fc245e)), closes [#81](https://github.com/hermanngeorge15/qawave-automation/issues/81)
* **ci:** Fix release-please configuration ([#87](https://github.com/hermanngeorge15/qawave-automation/issues/87)) ([b54f548](https://github.com/hermanngeorge15/qawave-automation/commit/b54f54811b241ca52ee865b4b59d24cb3e42af61))
* **ci:** Use Apache Kafka official image ([d6f2b3c](https://github.com/hermanngeorge15/qawave-automation/commit/d6f2b3c10305fc3ad1cd24e7d1ffa40f7d15c49b)), closes [#81](https://github.com/hermanngeorge15/qawave-automation/issues/81)
* **ci:** Use correct Kafka image tag ([e972e11](https://github.com/hermanngeorge15/qawave-automation/commit/e972e11306a261e172bb9788472de30e867b2b77)), closes [#81](https://github.com/hermanngeorge15/qawave-automation/issues/81)
