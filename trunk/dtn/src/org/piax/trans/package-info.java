/**
 * PIAXのTransport層を実現するパッケージ。
 * <p>
 * org.piax.trans 以下のパッケージは独立したパッケージとして上位アプリケーション
 * から使用することができる。
 * PIAX Transport層は、その実装にID/Locator分離の機構を取り入れている。
 * このため、上位アプリケーションは相手ピアとの通信に、Locatorだけでなく
 * IDを指定した通信も可能となる。
 * <p>
 * Transport層では、実際の通信に対象とする物理ネットワークに対応したService
 * Provider モジュールを使用する。通信モジュールは、plug-in 可能であり、
 * デフォルトでは、TCP, UDP, UDP hole punching を使ったNAT越え（いずれもIPv4前提）
 * とエミュレーションをサポートしている。
 * <p>
 * PIAX Transport層は、ヘテロジーニアスな物理ネットワークの環境に対応するための
 * 機構を持っている。これにより、新しい通信方式を持った無線などのネットワークが
 * 出現した場合にも通信モジュールを実装し、plug-inすることで対応可能となる。
 * 
 * @author     Mikio Yoshida
 */
package org.piax.trans;