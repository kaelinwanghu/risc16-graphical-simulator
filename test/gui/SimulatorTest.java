package gui;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import gui.facade.EngineFacade;
import gui.dialogs.MessageDialog;
import engine.execution.ProcessorState;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * Tests for the Simulator class.
 *
 * Tests EngineObserver callbacks (onStateChanged, onProgramLoaded, onAssemblyError,
 * onExecutionError, onHalt), the edit() method, and modification tracking.
 *
 * Requires non-headless display and classpath resources (src is on classpath per run-tests.sh).
 */
public class SimulatorTest {

    private Simulator simulator;

    @Before
    public void setUp() throws Exception {
        // Delete autosave to prevent JOptionPane dialog in constructor
        Path autoSavePath = Paths.get(System.getProperty("user.home"),
            ".risc16-simulator", "autosave.as16");
        Files.deleteIfExists(autoSavePath);

        simulator = new Simulator();

        // Replace errorDialog with mock to prevent actual dialogs
        simulator.errorDialog = new MockMessageDialog(simulator);
    }

    @After
    public void tearDown() {
        if (simulator != null) {
            simulator.dispose();
        }
    }

    // ========================================================================
    // Mock MessageDialog
    // ========================================================================

    private static class MockMessageDialog extends MessageDialog {
        String lastError = null;
        int errorCount = 0;

        MockMessageDialog(JFrame parent) {
            super(parent);
        }

        @Override
        public void showError(String message) {
            lastError = message;
            errorCount++;
        }
    }

    // ========================================================================
    // Reflection helpers
    // ========================================================================

    private JButton getButton(String name) throws Exception {
        Field field = Simulator.class.getDeclaredField(name);
        field.setAccessible(true);
        return (JButton) field.get(simulator);
    }

    private EngineFacade getEngineFacade() {
        return simulator.getEngineFacade();
    }

    private InputPanel getInputPanel() throws Exception {
        Field field = Simulator.class.getDeclaredField("inputPanel");
        field.setAccessible(true);
        return (InputPanel) field.get(simulator);
    }

    private void assembleProgram(String source) throws Exception {
        InputPanel inputPanel = getInputPanel();
        inputPanel.setProgram(source);
        getEngineFacade().assemble(source);
    }

    // ========================================================================
    // onHalt tests
    // ========================================================================

    @Test
    public void testOnHaltDisablesExecuteButtons() throws Exception {
        simulator.onHalt();

        assertFalse(getButton("execute").isEnabled());
        assertFalse(getButton("executeStep").isEnabled());
    }

    @Test
    public void testOnHaltEnablesAssembleButton() throws Exception {
        simulator.onHalt();

        assertTrue(getButton("assemble").isEnabled());
    }

    // ========================================================================
    // onProgramLoaded tests
    // ========================================================================

    @Test
    public void testOnProgramLoadedEnablesExecuteButtons() throws Exception {
        String source = "addi r1, r0, 5\njalr r0, r0";
        assembleProgram(source);

        assertTrue(getButton("execute").isEnabled());
        assertTrue(getButton("executeStep").isEnabled());
    }

    @Test
    public void testOnProgramLoadedDisablesAssembleButton() throws Exception {
        String source = "addi r1, r0, 5\njalr r0, r0";
        assembleProgram(source);

        assertFalse(getButton("assemble").isEnabled());
    }

    @Test
    public void testOnProgramLoadedEnablesEditButton() throws Exception {
        String source = "addi r1, r0, 5\njalr r0, r0";
        assembleProgram(source);

        assertTrue(getButton("edit").isEnabled());
    }

    @Test
    public void testOnProgramLoadedCreatesAssemblyPanel() throws Exception {
        String source = "addi r1, r0, 5\njalr r0, r0";
        assembleProgram(source);

        assertNotNull(simulator.assemblyPanel);
    }

    // ========================================================================
    // onStateChanged tests
    // ========================================================================

    @Test
    public void testOnStateChangedDisablesButtonsWhenHalted() throws Exception {
        ProcessorState haltedState = ProcessorState.builder()
            .setHalted(true)
            .build();

        // Enable execute buttons first
        getButton("execute").setEnabled(true);
        getButton("executeStep").setEnabled(true);

        simulator.onStateChanged(null, haltedState);

        assertFalse(getButton("execute").isEnabled());
        assertFalse(getButton("executeStep").isEnabled());
    }

    // ========================================================================
    // onAssemblyError tests
    // ========================================================================

    @Test
    public void testOnAssemblyErrorShowsFormattedMessage() throws Exception {
        String source = "invalid r1, r2, r3";
        getEngineFacade().assemble(source);

        MockMessageDialog mock = (MockMessageDialog) simulator.errorDialog;
        assertNotNull(mock.lastError);
        assertTrue(mock.lastError.contains("error"));
    }

    @Test
    public void testOnAssemblyErrorClearsFacade() throws Exception {
        // First assemble a valid program
        String validSource = "addi r1, r0, 5\njalr r0, r0";
        getEngineFacade().assemble(validSource);

        // Now assemble invalid code - should trigger clear
        String invalidSource = "invalid instruction";
        getEngineFacade().assemble(invalidSource);

        assertEquals(0, getEngineFacade().getPC());
    }

    // ========================================================================
    // onExecutionError tests
    // ========================================================================

    @Test
    public void testOnExecutionErrorShowsErrorMessage() throws Exception {
        engine.execution.ExecutionException error =
            new engine.execution.ExecutionException("Test error", 0);

        simulator.onExecutionError(error);

        MockMessageDialog mock = (MockMessageDialog) simulator.errorDialog;
        assertNotNull(mock.lastError);
        assertTrue(mock.lastError.contains("Test error"));
    }

    @Test
    public void testOnExecutionErrorDisablesExecuteButtons() throws Exception {
        // Enable them first
        getButton("execute").setEnabled(true);
        getButton("executeStep").setEnabled(true);

        engine.execution.ExecutionException error =
            new engine.execution.ExecutionException("Test error", 0);
        simulator.onExecutionError(error);

        assertFalse(getButton("execute").isEnabled());
        assertFalse(getButton("executeStep").isEnabled());
    }

    // ========================================================================
    // edit() tests
    // ========================================================================

    @Test
    public void testEditClearClearsInputAndFacade() throws Exception {
        InputPanel inputPanel = getInputPanel();
        inputPanel.setProgram("some code");

        simulator.edit(true);

        assertEquals("", inputPanel.getProgram());
        assertEquals(0, getEngineFacade().getPC());
    }

    @Test
    public void testEditNoClearPreservesInput() throws Exception {
        InputPanel inputPanel = getInputPanel();
        inputPanel.setProgram("some code");

        simulator.edit(false);

        assertEquals("some code", inputPanel.getProgram());
    }

    @Test
    public void testEditDisablesExecuteAndEditButtons() throws Exception {
        // Enable them first
        getButton("execute").setEnabled(true);
        getButton("executeStep").setEnabled(true);
        getButton("edit").setEnabled(true);

        simulator.edit(false);

        assertFalse(getButton("execute").isEnabled());
        assertFalse(getButton("executeStep").isEnabled());
        assertFalse(getButton("edit").isEnabled());
    }

    @Test
    public void testEditEnablesAssembleButton() throws Exception {
        getButton("assemble").setEnabled(false);

        simulator.edit(false);

        assertTrue(getButton("assemble").isEnabled());
    }

    // ========================================================================
    // setModified / isModified tests
    // ========================================================================

    @Test
    public void testSetModifiedAndIsModified() {
        assertFalse(simulator.isModified());

        simulator.setModified(true);
        assertTrue(simulator.isModified());

        simulator.setModified(false);
        assertFalse(simulator.isModified());
    }
}
