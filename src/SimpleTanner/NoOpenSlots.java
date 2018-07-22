package SimpleTanner;

class NoOpenSlots extends Exception {

    NoOpenSlots(){
        super("Cannot sell or buy item. You have no open grand exchange slots.");
    }

}
