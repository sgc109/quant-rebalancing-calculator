package portfolio.rebalancer

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode

/**
 * Kotest 의 Project Level Config.
 * AbstractProjectConfig 를 상속하는 Classpath 내 모든 class 를 감지하여
 * 내부에 정의된 설정들을 모두 적용하며, Spec 이나 각 테스트 내부에서 override 가 가능.
 *
 * 공식 문서: https://kotest.io/docs/framework/project-config.html
 */
class KotestProjectConfig : AbstractProjectConfig() {
    /**
     * Kotest 을 testing framework 로 사용하는 클래스의 가장 안쪽 테스트들을 실행할 때마다 새로운 Spec class 인스턴스를 생성하도록 하여
     * 각 테스트가 격리될 수 있도록 하기 위함.
     */
    override val isolationMode = IsolationMode.InstancePerLeaf
}
