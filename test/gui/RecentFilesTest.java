package gui;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Tests for the RecentFiles class.
 *
 * Tests recent file list management: ordering, max size, duplicates, filtering.
 * Cleans preference keys before and after each test to avoid pollution.
 */
public class RecentFilesTest {

    private RecentFiles recentFiles;
    private File tempDir;
    private List<File> tempFiles;

    @Before
    public void setUp() throws IOException {
        // Clear preferences to prevent pollution
        clearPreferences();

        tempDir = Files.createTempDirectory("recentfiles-test").toFile();
        tempFiles = new ArrayList<>();

        recentFiles = new RecentFiles();
    }

    @After
    public void tearDown() {
        // Clean up temp files
        for (File f : tempFiles) {
            f.delete();
        }
        if (tempDir != null && tempDir.exists()) {
            tempDir.delete();
        }

        // Clear preferences
        clearPreferences();
    }

    private void clearPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(RecentFiles.class);
        for (int i = 0; i < 5; i++) {
            prefs.remove("recent_file_" + i);
        }
    }

    private File createTempFile(String name) throws IOException {
        File file = new File(tempDir, name);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("content");
        }
        tempFiles.add(file);
        return file;
    }

    // ========================================================================
    // addFile tests
    // ========================================================================

    @Test
    public void testAddFileSingleFile() throws IOException {
        File file = createTempFile("a.as16");
        recentFiles.addFile(file);

        List<File> list = recentFiles.getRecentFiles();
        assertEquals(1, list.size());
        assertEquals(file, list.get(0));
    }

    @Test
    public void testAddFileMultipleFiles() throws IOException {
        File a = createTempFile("a.as16");
        File b = createTempFile("b.as16");
        recentFiles.addFile(a);
        recentFiles.addFile(b);

        List<File> list = recentFiles.getRecentFiles();
        assertEquals(2, list.size());
    }

    @Test
    public void testAddFileMostRecentFirst() throws IOException {
        File a = createTempFile("a.as16");
        File b = createTempFile("b.as16");
        recentFiles.addFile(a);
        recentFiles.addFile(b);

        List<File> list = recentFiles.getRecentFiles();
        assertEquals(b, list.get(0));
        assertEquals(a, list.get(1));
    }

    @Test
    public void testAddFileMaxSizeFive() throws IOException {
        File[] files = new File[6];
        for (int i = 0; i < 6; i++) {
            files[i] = createTempFile("file" + i + ".as16");
            recentFiles.addFile(files[i]);
        }

        List<File> list = recentFiles.getRecentFiles();
        assertEquals(5, list.size());
        // Oldest (files[0]) should be dropped
        assertFalse(list.contains(files[0]));
        // Most recent should be first
        assertEquals(files[5], list.get(0));
    }

    @Test
    public void testAddFileDuplicateMovesToFront() throws IOException {
        File a = createTempFile("a.as16");
        File b = createTempFile("b.as16");
        recentFiles.addFile(a);
        recentFiles.addFile(b);
        recentFiles.addFile(a);

        List<File> list = recentFiles.getRecentFiles();
        assertEquals(2, list.size());
        assertEquals(a, list.get(0));
        assertEquals(b, list.get(1));
    }

    @Test
    public void testAddFileNullIsIgnored() {
        recentFiles.addFile(null);

        List<File> list = recentFiles.getRecentFiles();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testAddFileNonExistentIsIgnored() {
        File nonExistent = new File(tempDir, "no-such-file.as16");
        recentFiles.addFile(nonExistent);

        List<File> list = recentFiles.getRecentFiles();
        assertTrue(list.isEmpty());
    }

    // ========================================================================
    // getRecentFiles tests
    // ========================================================================

    @Test
    public void testGetRecentFilesFiltersDeleted() throws IOException {
        File a = createTempFile("a.as16");
        File b = createTempFile("b.as16");
        recentFiles.addFile(a);
        recentFiles.addFile(b);

        // Delete file a
        a.delete();

        List<File> list = recentFiles.getRecentFiles();
        assertEquals(1, list.size());
        assertEquals(b, list.get(0));
    }

    @Test
    public void testGetRecentFilesReturnsDefensiveCopy() throws IOException {
        File a = createTempFile("a.as16");
        recentFiles.addFile(a);

        List<File> list = recentFiles.getRecentFiles();
        list.clear();

        // Original list should be unaffected
        List<File> list2 = recentFiles.getRecentFiles();
        assertEquals(1, list2.size());
    }

    // ========================================================================
    // clear tests
    // ========================================================================

    @Test
    public void testClearRemovesAll() throws IOException {
        File a = createTempFile("a.as16");
        File b = createTempFile("b.as16");
        recentFiles.addFile(a);
        recentFiles.addFile(b);

        recentFiles.clear();

        List<File> list = recentFiles.getRecentFiles();
        assertTrue(list.isEmpty());
    }
}
