package commands;

import access.Access;
import exception.InvalidFileFormatException;
import manager.card.Card;
import manager.chapter.Chapter;
import scheduler.Scheduler;
import storage.Storage;
import ui.Ui;

import java.io.FileNotFoundException;
import java.util.ArrayList;


/**
 * Starts revision for a particular chapter.
 */
public class ReviseCommand extends Command {
    public static final String COMMAND_WORD = "revise";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Starts revision based on a particular chapter. \n"
            + "Parameters: INDEX_OF_CHAPTER\n" + "Example: " + COMMAND_WORD + " 2\n";

    public static final String MESSAGE_SUCCESS = "You have completed revision for %1$s.";
    public static final String MESSAGE_NO_CARDS_IN_CHAPTER = "You currently have no cards in %1$s.";
    public static final String MESSAGE_NO_CARDS_DUE = "You have no cards due for revision in %1$s today.";
    public static final String MESSAGE_SHOW_ANSWER_PROMPT = "\n[enter s to show answer]";
    public static final String MESSAGE_SHOW_RATING_PROMPT = "How well did you do for this card?\n"
            + "[enter e(easy), m(medium), h(hard), c(cannot answer)]";
    public static final String EASY = "e";
    public static final String MEDIUM = "m";
    public static final String HARD = "h";
    public static final String CANNOT_ANSWER = "c";

    private final int reviseIndex;

    public ReviseCommand(int reviseIndex) {
        this.reviseIndex = reviseIndex;
    }

    public Chapter getChapter(int reviseIndex, Access access, Ui ui) throws IndexOutOfBoundsException {
        Chapter chapter;
        try {
            chapter = access.getModule().getChapters().getChapter(reviseIndex);
            return chapter;
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("The chapter is not found.\n");
        } 
    }

    private ArrayList<Card> getCards(Ui ui, Access access, Storage storage, Chapter toRevise)
            throws FileNotFoundException {
        ArrayList<Card> allCards;
        try {
            allCards = storage.loadCard(access.getModuleLevel(), toRevise.getChapterName());
            toRevise.setCards(allCards);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("File is not found.\n");
        }
        return allCards;
    }

    private int reviseCard(int count, Card c, Ui ui, ArrayList<Card> repeatCards) {
        ui.showToUser("\nQuestion " + count + ":");
        ui.showCardRevision(c);
        String input = ui.getRating();
        rateCard(ui, repeatCards, c, input);
        return count++;
    }

    @Override
    public void execute(CardList cards, Ui ui, Access access, Storage storage) 
        throws FileNotFoundException {
        Chapter toRevise = getChapter(reviseIndex, access, ui);
        if (!Scheduler.isDeadlineDue(toRevise.getDueBy())) {
            return;
        }

        ArrayList<Card> repeatCards = new ArrayList<>();

        int cardCount = allCards.size();
        if (cardCount == 0) {
            ui.showToUser(String.format(MESSAGE_NO_CARDS_IN_CHAPTER, toRevise));
            return;
        }

        ui.showToUser("The revision for " + toRevise + " will start now:");

        int count = 1;
        ArrayList<Card> allCards = getCards(ui, access, storage, toRevise);

        for (Card c : allCards) {
            count = reviseCard(count, c, ui, repeatCards);
        }
        int remainingCards = repeatRevision(ui, repeatCards, count);
        assert remainingCards == 0 : "Cards were left in repeat revision";
        ui.showToUser(String.format(MESSAGE_SUCCESS, toRevise));
        toRevise.setDueBy(Scheduler.computeDeckDeadline(toRevise.getCards()), storage, access);

    }

    public static ArrayList<Card> rateCard(Ui ui, ArrayList<Card> repeatCards, Card c, String input) {
        boolean isInvalid = true;

        while (isInvalid) {
            switch (input.trim().toLowerCase()) {
            case EASY:
                c.setPreviousInterval(Scheduler.computeEasyInterval(c.getPreviousInterval()));
                isInvalid = false;
                break;
            case MEDIUM:
                c.setPreviousInterval(Scheduler.computeMediumInterval(c.getPreviousInterval()));
                isInvalid = false;
                break;
            case HARD:
                c.setPreviousInterval(Scheduler.computeHardInterval(c.getPreviousInterval()));
                isInvalid = false;
                break;
            case CANNOT_ANSWER:
                repeatCards.add(c);
                isInvalid = false;
                break;
            default:
                ui.showToUser("You have entered an invalid input, please try again.");
                input = ui.getRating();
            }
        }
        return repeatCards;
    }

    private int repeatRevision(Ui ui, ArrayList<Card> cards, int count) {
        while (cards.size() != 0) {
            ArrayList<Card> repeatCards = new ArrayList<>();
            for (Card c : cards) {
                reviseCard(count, c, ui, repeatCards);
            }
            cards = new ArrayList<>(repeatCards);
        }
        return cards.size();
    }

    @Override
    public boolean isExit() {
        return false;
    }
}
