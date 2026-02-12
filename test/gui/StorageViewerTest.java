package gui;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import gui.facade.EngineFacade;
import gui.components.ResizableTable;
import engine.execution.ProcessorState;

import java.lang.reflect.Field;
import java.util.Set;

import javax.swing.JTextArea;

/**
 * Tests for the StorageViewer class.
 *
 * Tests register change tracking, value formatting, and display data construction.
 * Uses reflection to access private fields since there are no public getters.
 */
public class StorageViewerTest {

    private StorageViewer storageViewer;
    private EngineFacade facade;

    @Before
    public void setUp() {
        facade = new EngineFacade(1024);
        storageViewer = new StorageViewer(null, facade);
    }

    // ========================================================================
    // Reflection helpers
    // ========================================================================

    @SuppressWarnings("unchecked")
    private Set<Integer> getChangedRegisters() throws Exception {
        Field field = StorageViewer.class.getDeclaredField("changedRegisters");
        field.setAccessible(true);
        return (Set<Integer>) field.get(storageViewer);
    }

    private short[] getLastRegisterValues() throws Exception {
        Field field = StorageViewer.class.getDeclaredField("lastRegisterValues");
        field.setAccessible(true);
        return (short[]) field.get(storageViewer);
    }

    private JTextArea getData() throws Exception {
        Field field = StorageViewer.class.getDeclaredField("data");
        field.setAccessible(true);
        return (JTextArea) field.get(storageViewer);
    }

    private ResizableTable getResizableTable() throws Exception {
        Field field = StorageViewer.class.getDeclaredField("resizableTable");
        field.setAccessible(true);
        return (ResizableTable) field.get(storageViewer);
    }

    // ========================================================================
    // Constructor tests
    // ========================================================================

    @Test
    public void testConstructorInitializesLastRegisterValues() throws Exception {
        short[] lastValues = getLastRegisterValues();
        assertEquals(8, lastValues.length);
        for (int i = 0; i < 8; i++) {
            assertEquals(0, lastValues[i]);
        }
    }

    // ========================================================================
    // updateState change tracking tests
    // ========================================================================

    @Test
    public void testUpdateStateTracksChangedRegisters() throws Exception {
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 42)
            .build();

        storageViewer.updateState(state);

        Set<Integer> changed = getChangedRegisters();
        assertTrue(changed.contains(1));
    }

    @Test
    public void testUpdateStateNoChangeWhenSameValues() throws Exception {
        ProcessorState state = ProcessorState.builder().build();

        storageViewer.updateState(state);

        Set<Integer> changed = getChangedRegisters();
        assertTrue(changed.isEmpty());
    }

    @Test
    public void testUpdateStateTracksMultipleChanges() throws Exception {
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .setRegister(3, (short) 30)
            .setRegister(5, (short) 50)
            .build();

        storageViewer.updateState(state);

        Set<Integer> changed = getChangedRegisters();
        assertTrue(changed.contains(1));
        assertTrue(changed.contains(3));
        assertTrue(changed.contains(5));
        assertEquals(3, changed.size());
    }

    @Test
    public void testUpdateStateClearsOldChangesFirst() throws Exception {
        // First update: R1 changes
        ProcessorState state1 = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .build();
        storageViewer.updateState(state1);

        // Second update: R2 changes (R1 stays the same)
        ProcessorState state2 = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .setRegister(2, (short) 20)
            .build();
        storageViewer.updateState(state2);

        Set<Integer> changed = getChangedRegisters();
        assertFalse(changed.contains(1));
        assertTrue(changed.contains(2));
    }

    // ========================================================================
    // clearChanges tests
    // ========================================================================

    @Test
    public void testClearChangesResetsTracking() throws Exception {
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 42)
            .build();
        storageViewer.updateState(state);

        storageViewer.clearChanges();

        Set<Integer> changed = getChangedRegisters();
        assertTrue(changed.isEmpty());
    }

    // ========================================================================
    // refresh / display tests
    // ========================================================================

    @Test
    public void testRefreshDisplaysRegistersInfoText() throws Exception {
        storageViewer.refresh();

        JTextArea data = getData();
        String text = data.getText();
        assertTrue(text.contains("PC:"));
        assertTrue(text.contains("Instructions:"));
        assertTrue(text.contains("Halted:"));
    }

    @Test
    public void testRefreshDisplaysHaltedNo() throws Exception {
        storageViewer.refresh();

        JTextArea data = getData();
        assertTrue(data.getText().contains("Halted: No"));
    }

    @Test
    public void testRefreshDisplaysHaltedYes() throws Exception {
        // Assemble and run a halt instruction to make engine halted
        facade.assemble("jalr r0, r0");
        facade.run();

        storageViewer.refresh();

        JTextArea data = getData();
        assertTrue(data.getText().contains("Halted: Yes"));
    }

    @Test
    public void testDisplayRegistersShowsAllEightRegisters() throws Exception {
        storageViewer.refresh();

        ResizableTable table = getResizableTable();
        assertEquals(8, table.getRowCount());
    }

    // ========================================================================
    // lastRegisterValues update tests
    // ========================================================================

    @Test
    public void testUpdateStateUpdatesLastRegisterValues() throws Exception {
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 42)
            .setRegister(3, (short) 99)
            .build();

        storageViewer.updateState(state);

        short[] lastValues = getLastRegisterValues();
        assertEquals(42, lastValues[1]);
        assertEquals(99, lastValues[3]);
    }

    @Test
    public void testSequentialUpdatesTrackChangesCorrectly() throws Exception {
        // Step 1: R1 changes
        ProcessorState state1 = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .build();
        storageViewer.updateState(state1);

        Set<Integer> changed1 = getChangedRegisters();
        assertTrue(changed1.contains(1));
        assertFalse(changed1.contains(2));

        // Step 2: R2 changes, R1 stays same
        ProcessorState state2 = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .setRegister(2, (short) 20)
            .build();
        storageViewer.updateState(state2);

        Set<Integer> changed2 = getChangedRegisters();
        assertFalse(changed2.contains(1));
        assertTrue(changed2.contains(2));
    }
}
