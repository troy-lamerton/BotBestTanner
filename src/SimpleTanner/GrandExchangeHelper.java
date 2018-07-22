package SimpleTanner;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankMode;
import org.dreambot.api.methods.grandexchange.GrandExchange;
import org.dreambot.api.methods.walking.web.node.impl.bank.WebBankArea;
import org.dreambot.api.script.AbstractScript;

class GrandExchangeHelper {
    private AbstractScript ctx;
    private int sellingInSlot = -1;

    GrandExchangeHelper (AbstractScript main) {
        this.ctx = main;
    }

    boolean goToGrandExchange() {
        if (this.isAtGrandExchange()) {
            this.log("At grand exchange");
            return true;
        } else if (ctx.getWalking().walk(WebBankArea.GRAND_EXCHANGE.getArea().getRandomTile())) {
            return AbstractScript.sleepUntil(() -> WebBankArea.GRAND_EXCHANGE.getArea().contains(ctx.getLocalPlayer()), 2000);
        }
        return false;
    }

    boolean isAtGrandExchange() {
        return WebBankArea.GRAND_EXCHANGE.getArea().contains(ctx.getLocalPlayer());
    }

    boolean withdrawAllNoted(String item) {
        Bank b = ctx.getBank();
        if (ctx.getInventory().contains(item)) {
            return true;
        } else {
            if (!b.isOpen() && !ctx.getLocalPlayer().isMoving() && b.openClosest()) {
                if (AbstractScript.sleepUntil(b::isOpen, 8000)) {
                    if (b.setWithdrawMode(BankMode.NOTE) && AbstractScript.sleepUntil(() -> b.getWithdrawMode() == BankMode.NOTE, 8000)) {
                        if (b.withdrawAll(item)) {
                            AbstractScript.sleep(Calculations.random(100, 300));
                            return AbstractScript.sleepUntil(b::close, 8000);
                        }
                    }
                }
            }
        }
        return false;
    }

    boolean sellAll(String sellItem, int price, int timeoutMs) throws NoOpenSlots {
        if(AbstractScript.sleepUntil(() -> ctx.getInventory().contains(item -> item != null && item.getName().equals(sellItem)), 8000)) {
            // item withdrawn
            GrandExchange ge = ctx.getGrandExchange();
            // open exchange and sell item
            int sellAmount = ctx.getInventory().get(sellItem).getAmount();
            if (!ge.isOpen()) {
                ge.open();
            } else {
                if (AbstractScript.sleepUntil(ge::isOpen, 8000)) {
                    if (this.sellingInSlot < 0) {
                        // not yet selling
                        this.sellingInSlot = ge.getFirstOpenSlot();
                        if (ge.openSellScreen(this.sellingInSlot)) {
                            // sell all leather
                            if (ge.sellItem(sellItem, sellAmount, price)) {
                                AbstractScript.sleep(Calculations.random(100, 300));
                                ge.confirm();
                                // waiting for sale to complete
                                if (AbstractScript.sleepUntil(() -> ge.isReadyToCollect(this.sellingInSlot), Calculations.random(timeoutMs - 1000, timeoutMs + 1000))) {
                                    if (ge.isReadyToCollect(this.sellingInSlot)) {
                                        return ge.collect();
                                    } else {
                                        // timed out
                                        return false;
                                    }
                                }

                            }
                        } else {
                            log("Cannot sell anything. All Grand Exchange slots are full.");
                            throw new NoOpenSlots();
                        }
                    } else {
                        log("already selling, why you call sellAll again?");
                    }
                }
            }
        }
        return false;
    }
    boolean sellAll(String sellItem, int price) throws NoOpenSlots {
        return sellAll(sellItem, price, 15000);
    }

    boolean abortCurrentSell() {
        if (this.sellingInSlot < 0) {
            return true;
        }

        GrandExchange ge = ctx.getGrandExchange();
        if (ge.cancelOffer(this.sellingInSlot)) {
            if (ge.collect()) {
                return AbstractScript.sleepUntil(() -> !ge.isReadyToCollect(this.sellingInSlot), 8000);
            }
        }
        return false;
    }

    private void log(String str) {
        AbstractScript.log(str);
    }
}