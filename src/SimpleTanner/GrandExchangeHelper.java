package SimpleTanner;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankMode;
import org.dreambot.api.methods.grandexchange.GrandExchange;
import org.dreambot.api.methods.walking.web.node.impl.bank.WebBankArea;
import org.dreambot.api.script.AbstractScript;

class GrandExchangeHelper {
    public AbstractScript ctx;
    private int sellingInSlot = -1;
    private int buyingInSlot = -1;

    GrandExchangeHelper (AbstractScript main) {
        this.ctx = main;
    }

    boolean goToGrandExchange() {
        if (this.isAtGrandExchange()) {
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
        return false;
    }

    boolean sellAll(String sellItem, int price, int timeoutMs) throws NoOpenSlots {
        log("sell all " + sellItem);

        if(AbstractScript.sleepUntil(() -> ctx.getInventory().contains(item -> item != null && item.getName().equals(sellItem)), 8000)) {
            // item withdrawn
            GrandExchange ge = ctx.getGrandExchange();
            // open exchange and sell item
            int sellAmount = ctx.getInventory().get(sellItem).getAmount();
            if (!ge.isOpen()) {
                ge.open();
            }
            if (AbstractScript.sleepUntil(ge::isOpen, 8000)) {
                if (this.sellingInSlot < 0) {
                    // not yet selling
                    this.sellingInSlot = ge.getFirstOpenSlot();
                    if (ge.openSellScreen(this.sellingInSlot)) {
                        // sell all
                        if (ge.sellItem(sellItem, sellAmount, price)) {
                            AbstractScript.sleep(Calculations.random(100, 300));
                            ge.confirm();
                            // waiting for sale to complete
                            if (AbstractScript.sleepUntil(() -> ge.isReadyToCollect(this.sellingInSlot), Calculations.random(timeoutMs - 1000, timeoutMs + 1000))) {
                                if (ge.isReadyToCollect(this.sellingInSlot)) {
                                    sleepRandom();
                                    this.sellingInSlot = -1;
                                    return ge.collect();
                                } else {
                                    // timed out
                                    return false;
                                }
                            }

                        }
                    } else {
                        throw new NoOpenSlots();
                    }
                } else {
                    log("already selling, why you call sellAll again?");
                }
            }
        }
        return false;
    }
    boolean sellAll(String sellItem, int price) throws NoOpenSlots {
        return sellAll(sellItem, price, 15000);
    }

    boolean buy(String buyItem, int price, int amount, int timeoutMs) throws NoOpenSlots {
        log("buy " + buyItem);

        if (!ctx.getInventory().contains(995)) {
            log("Failed to buy: No coins in inventory");
            return false;
        }

        GrandExchange ge = ctx.getGrandExchange();

        // open exchange and buy item
        if (!ge.isOpen()) {
            ge.open();
        }
        if (AbstractScript.sleepUntil(ge::isOpen, 8000)) {
            if (this.buyingInSlot < 0) {
                this.buyingInSlot = ge.getFirstOpenSlot();
                if (ge.openBuyScreen(this.buyingInSlot)) {
                    // buy item
                    if (ge.buyItem(buyItem, amount, price)) {
                        AbstractScript.sleep(Calculations.random(100, 300));
                        ge.confirm();
                        // waiting for buy to complete
                        if (AbstractScript.sleepUntil(() -> ge.isReadyToCollect(this.buyingInSlot), Calculations.random(timeoutMs - 1000, timeoutMs + 1000))) {
                            if (ge.isReadyToCollect(this.buyingInSlot)) {
                                sleepRandom();
                                this.buyingInSlot = -1;
                                return ge.collect();
                            } else {
                                // timed out
                                return false;
                            }
                        }
                    }
                } else {
                    throw new NoOpenSlots();
                }
            } else {
                log("already buying, why you call buy again?");
            }
        }
        return false;
    }
    boolean buy(String buyItem, int price, int amount) throws NoOpenSlots {
        return buy(buyItem, price, amount, 10000);
    }

    boolean abortCurrentSell() {
        if (abortOffer(this.sellingInSlot)) {
            this.sellingInSlot = -1;
            return true;
        }
        return false;
    }

    boolean abortCurrentBuy() {
        if (abortOffer(this.buyingInSlot)) {
            this.buyingInSlot = -1;
            return true;
        }
        return false;
    }

    private boolean abortOffer(int slot) {
        if (slot < 0) {
            return true;
        }

        GrandExchange ge = ctx.getGrandExchange();

        if (!ge.slotContainsItem(slot)) {
            return true;
        }

        if (ge.cancelOffer(slot)) {
            sleepRandom();
            if (ge.goBack()) {
                sleepRandom();
                if (AbstractScript.sleepUntil(() -> ge.isGeneralOpen() && ge.isReadyToCollect(slot), 8000)) {
                    if (ge.collect()) {
                        if (AbstractScript.sleepUntil(() -> !ge.slotContainsItem(slot), 8000)) {
                            return true;
                        } else {
                            log("FAILED: slot contains item");
                        }
                    } else {
                        log("FAILED: collect() interaction");
                    }
                } else {
                    log("FAILED: general not open / not ready to collect");
                }
            } else {
                log("FAILED: goBack() interaction");
            }
        } else {
            log("FAILED: cancelOffer(slot)");
        }
        log("FAILED ABORT WTF???");

        return false; // failed to abort
    }

    private void sleepRandom() {
        AbstractScript.sleep(200, 400);
    }

    private void log(String str) {
        AbstractScript.log(str);
    }
}