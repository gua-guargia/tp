package commands;

import access.Access;
import manager.admin.Admin;
import manager.card.Card;
import manager.chapter.Chapter;
import manager.module.Module;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import storage.Storage;
import ui.Ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import static commands.ReviseCommand.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReviseCommandTest {
    public static final String INVALID_INPUT = "You have entered an invalid input, please try again.\r\n";
    private final InputStream systemIn = System.in;
    private final PrintStream systemOut = System.out;

    private ByteArrayInputStream testIn;
    private ByteArrayOutputStream testOut;

    private Ui ui;
    private Storage storage;
    private Access access;
    private File moduleDir;
    private File chapterFile;
    private File chapterFile2;
    private Card card;

    @BeforeEach
    public void init() throws IOException {
        ui = new Ui();
        String filePath = "src/test/data/admin";
        storage = new Storage(filePath);
        ArrayList<Chapter> chapters = new ArrayList<>();
        ArrayList<Card> cards = new ArrayList<>();
        Chapter chapter = new Chapter("chapter1");
        Chapter chapter2 = new Chapter("chapter2");
        card = new Card("1+1", "2", 4, 1);
        cards.add(card);
        chapter.setCards(cards);
        chapters.add(chapter);
        chapters.add(chapter2);
        Module module = new Module("CS2113T");
        module.setChapters(chapters);
        ArrayList<Module> modules = new ArrayList<>();
        modules.add(module);
        Admin admin = new Admin(modules);
        access = new Access();
        access.setAdmin(admin);
        access.setModuleLevel("CS2113T");
        access.setModule(module);
        access.setIsModuleLevel();
        moduleDir = new File(filePath + "/CS2113T");
        chapterFile = new File(filePath + "/CS2113T/chapter1.txt");
        chapterFile2 = new File(filePath + "/CS2113T/chapter2.txt");
        moduleDir.mkdir();
        chapterFile.createNewFile();
        chapterFile2.createNewFile();
        FileWriter fw = new FileWriter(filePath + "/CS2113T/chapter1.txt");
        fw.write("[Q] 1+1 | [A] 2 | [P] 4 | [R] 1");
        fw.close();
    }

    @BeforeEach
    public void setUpOutput() {
        testOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(testOut));
    }

    private void provideInput(String data) {
        testIn = new ByteArrayInputStream(data.getBytes());
        System.setIn(testIn);
    }

    private String getOutput() {
        return testOut.toString();
    }

    @AfterEach
    public void restoreSystemInputOutput() {
        System.setIn(systemIn);
        System.setOut(systemOut);
    }

    @AfterEach
    public void end() {
        if (chapterFile.exists()) {
            chapterFile.delete();
        }
        if (moduleDir.exists()) {
            moduleDir.delete();
        }
    }

    @Test
    public void execute_invalidIndex_expectException() {
        ReviseCommand command = new ReviseCommand(2);
        assertThrows(IndexOutOfBoundsException.class, () -> command.execute(ui, access, storage));
    }

    @Test
    public void execute_validIndexUserInputN_expectNoOutput() throws IOException {
        ReviseCommand command = new ReviseCommand(0);
        String testInput = "w\nN";
        provideInput(testInput);
        command.execute(ui, access, storage);
        String expectedResult = String.format(MESSAGE_CHAPTER_NOT_DUE, "chapter1")
                + MESSAGE_SHOW_REVISE_PROMPT + "\r\n" + INVALID_INPUT
                + String.format(MESSAGE_NOT_REVISING, "chapter1");
        assertEquals(expectedResult, getOutput().trim());
    }

    @Test
    public void execute_validIndexUserInputY_expectNoOutput() throws IOException {
        ReviseCommand command = new ReviseCommand(0);
        String testInput = "w\nY\nw\ns\nw\nc\ns\ne";
        provideInput(testInput);
        command.execute(ui, access, storage);
        String expectedResult = String.format(MESSAGE_CHAPTER_NOT_DUE, "chapter1")
                + MESSAGE_SHOW_REVISE_PROMPT + "\r\n" + INVALID_INPUT + "\nCard count: 1\r\n"
                + String.format(MESSAGE_START_REVISION, "chapter1") + "\r\n\nQuestion 1:\r\n"
                + card.getRevisionQuestion() + MESSAGE_SHOW_ANSWER_PROMPT + "\r\n"
                + INVALID_INPUT + card.getRevisionAnswer() + "\r\n" + MESSAGE_SHOW_RATING_PROMPT + "\r\n"
                + INVALID_INPUT + "\nQuestion 2:\r\n" + card.getRevisionQuestion()
                + MESSAGE_SHOW_ANSWER_PROMPT + "\r\n" + card.getRevisionAnswer() + "\r\n"
                + MESSAGE_SHOW_RATING_PROMPT + "\r\n" + String.format(MESSAGE_SUCCESS, "chapter1");
        assertEquals(expectedResult, getOutput().trim());
    }

    @Test
    public void execute_validIndexZeroCards_expectNoCardsForRevision() throws IOException {
        ReviseCommand command = new ReviseCommand(1);
        String testInput = "Y";
        provideInput(testInput);
        command.execute(ui, access, storage);
        String expectedResult = String.format(MESSAGE_CHAPTER_NOT_DUE, "chapter2")
                + MESSAGE_SHOW_REVISE_PROMPT + "\r\n" + "\nCard count: 0\r\n"
                + String.format(MESSAGE_NO_CARDS_IN_CHAPTER, "chapter2");
        assertEquals(expectedResult, getOutput().trim());
    }
}
