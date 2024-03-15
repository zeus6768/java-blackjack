package domain.participant.dealer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import static domain.BlackjackResultStatus.LOSE;
import static domain.BlackjackResultStatus.PUSH;
import static domain.BlackjackResultStatus.WIN;
import static domain.card.CardFixture.cardOf;
import static domain.card.CardFixture.cardsOf15;
import static domain.card.CardFixture.cardsOf20;
import static domain.card.CardFixture.cardsOf22;
import static domain.card.CardRank.ACE;
import static domain.card.CardRank.FIVE;
import static domain.card.CardRank.JACK;
import static domain.card.CardRank.NINE;
import static domain.card.CardRank.SEVEN;
import static domain.card.CardRank.SIX;
import static domain.card.CardRank.THREE;
import static domain.card.CardRank.TWO;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import domain.BlackjackResultStatus;
import domain.card.Card;
import domain.card.Cards;
import domain.participant.Participant;
import domain.participant.player.Player;
import domain.participant.player.PlayerFixture;
import domain.participant.player.Players;

class DealerTest {

    @Nested
    class DealTest {
        static Stream<Arguments> deal() {
            return Stream.of(
                    Arguments.of(cardOf(ACE), PlayerFixture.from("Zeus")),
                    Arguments.of(cardOf(TWO), new Dealer(Cards.emptyCards()))
            );
        }

        @DisplayName("딜러는 카드를 참가자에게 줄 수 있다.")
        @MethodSource
        @ParameterizedTest
        void deal(Card card, Participant participant) {
            Dealer dealer = new Dealer(Cards.from(List.of(card)));

            dealer.deal(participant);
            Cards hand = participant.hand();

            assertThat(hand.peek()).isEqualTo(card);
        }

        @DisplayName("딜러는 게임을 시작할 때 본인과 플레이어에게 카드를 나누어준다.")
        @Test
        void dealInitialCards() {
            Player player1 = PlayerFixture.from("제우스");
            Player player2 = PlayerFixture.from("조이썬");
            Cards cards = Cards.from(List.of(
                    cardOf(ACE), cardOf(THREE), cardOf(FIVE), cardOf(SEVEN), cardOf(NINE), cardOf(ACE)));
            Dealer dealer = new Dealer(cards);

            dealer.dealInitialCards(new Players(new LinkedHashSet<>(List.of(player1,  player2))));

            assertAll(
                    () -> assertThat(dealer.peek()).isEqualTo(cardOf(ACE)),
                    () -> assertThat(player1.hand().peek()).isEqualTo(cardOf(FIVE)),
                    () -> assertThat(player2.hand().peek()).isEqualTo(cardOf(NINE))
            );
        }

        static Stream<Arguments> dealInitialCardsSelf() {
            return Stream.of(
                    Arguments.of(Cards.from(List.of(cardOf(ACE), cardOf(ACE))), 12),
                    Arguments.of(Cards.from(List.of(cardOf(ACE), cardOf(TWO))), 13),
                    Arguments.of(Cards.from(List.of(cardOf(ACE), cardOf(JACK))), 21)
            );
        }

        @DisplayName("딜러는 처음에 받는 에이스 카드 한 장을 11로 계산한다.")
        @MethodSource
        @ParameterizedTest
        void dealInitialCardsSelf(Cards cards, int expected) {
            Dealer dealer = new Dealer(cards);
            dealer.deal(dealer, 2);
            assertThat(dealer.score()).isEqualTo(expected);
        }

        static Stream<Arguments> dealAdditionalCard() {
            return Stream.of(
                    Arguments.of(Cards.from(List.of(cardOf(FIVE), cardOf(FIVE), cardOf(ACE))), 21),
                    Arguments.of(Cards.from(List.of(cardOf(FIVE), cardOf(SIX), cardOf(ACE))), 12),
                    Arguments.of(Cards.from(List.of(cardOf(ACE), cardOf(SIX), cardOf(ACE))), 18)
            );
        }

        @DisplayName("딜러는 추가 에이스 카드를 11로 계산하되, 그렇게 할 때 버스트이거나 이미 에이스 카드를 보유한 경우 1로 계산한다.")
        @MethodSource
        @ParameterizedTest
        void dealAdditionalCard(Cards cards, int expected) {
            Dealer dealer = new Dealer(cards);
            dealer.deal(dealer, 3);
            assertThat(dealer.score()).isEqualTo(expected);
        }
    }

    static Stream<Arguments> isBust() {
        return Stream.of(
                Arguments.of(Cards.from(List.of(cardOf(ACE), cardOf(FIVE), cardOf(SIX))), 22, true),
                Arguments.of(Cards.from(List.of(cardOf(FIVE), cardOf(SIX), cardOf(ACE))), 12, false)
        );
    }

    @DisplayName("딜러의 카드 합계를 계산해 버스트를 판단한다.")
    @MethodSource
    @ParameterizedTest
    void isBust(Cards cards, int expectedTotal, boolean expectedBust) {
        Dealer dealer = new Dealer(cards);
        dealer.deal(dealer, 3);
        assertAll(
                () -> assertThat(dealer.score()).isEqualTo(expectedTotal),
                () -> assertThat(dealer.isBust()).isEqualTo(expectedBust)
        );
    }

    static Stream<Arguments> canHit() {
        return Stream.of(
                Arguments.of(Cards.from(List.of(cardOf(TWO), cardOf(THREE))), 5, true),
                Arguments.of(Cards.from(List.of(cardOf(ACE), cardOf(FIVE))), 16, true),
                Arguments.of(Cards.from(List.of(cardOf(ACE), cardOf(SIX))), 17, false)
        );
    }

    @DisplayName("딜러의 카드 합계를 계산해 추가로 카드를 받을 수 있는지 판단한다.")
    @MethodSource
    @ParameterizedTest
    void canHit(Cards cards, int expectedTotal, boolean expectedHit) {
        Dealer dealer = new Dealer(cards);
        dealer.deal(dealer, 2);
        assertAll(
                () -> assertThat(dealer.score()).isEqualTo(expectedTotal),
                () -> assertThat(dealer.canHit()).isEqualTo(expectedHit)
        );
    }

    static Stream<Arguments> resultStatusOf() {
        return Stream.of(
                Arguments.of(cardsOf22(), cardsOf22(), PUSH),
                Arguments.of(cardsOf22(), cardsOf20(), LOSE),
                Arguments.of(cardsOf20(), cardsOf22(), WIN),
                Arguments.of(cardsOf20(), cardsOf15(), WIN),
                Arguments.of(cardsOf15(), cardsOf20(), LOSE),
                Arguments.of(cardsOf20(), cardsOf20(), PUSH)
        );
    }

    @DisplayName("플레이어와 자신의 카드를 비교해 승패무를 정한다.")
    @MethodSource
    @ParameterizedTest
    void resultStatusOf(Cards dealerCards, Cards playerCards, BlackjackResultStatus expected) {
        Dealer dealer = new Dealer(Cards.emptyCards());
        Player player = PlayerFixture.from("hotea");
        receiveCards(dealer, dealerCards);
        receiveCards(player, playerCards);
        BlackjackResultStatus status = dealer.resultStatusAgainst(player);
        assertThat(status).isEqualTo(expected);
    }

    private void receiveCards(Participant participant, Cards cards) {
        cards.stream().forEach(participant::receive);
    }
}
