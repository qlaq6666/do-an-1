package com.example.n1;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.example.n1.HssvDatabase.Table.GIANGVIEN;
import static com.example.n1.HssvDatabase.Table.KETQUA;
import static com.example.n1.HssvDatabase.Table.LOP;
import static com.example.n1.HssvDatabase.Table.PHANCONG;
import static com.example.n1.HssvDatabase.Table.MONHOC;
import static com.example.n1.HssvDatabase.Table.SINHVIEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class HssvDatabaseTest {
    private static final String TEST_DB = "topic1-instrumentation.db";

    private Context context;
    private HssvDatabase database;

    @Before
    public void createDatabase() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(TEST_DB);
        database = new HssvDatabase(context, TEST_DB);
    }

    @After
    public void deleteDatabase() {
        database.close();
        context.deleteDatabase(TEST_DB);
    }

    @Test
    public void classesAndStudentsEnforceRelationshipsAndPersist() {
        database.insert(LOP, values("MALOP", "L01", "TENLOP", "Lớp 1"));
        database.insert(SINHVIEN, values("MASV", "SV01", "MALOP", "L01", "HOTEN", "Sinh viên 1"));
        database.insert(SINHVIEN, values("MASV", "SV02", "MALOP", "L01", "HOTEN", "Sinh viên 2"));
        assertEquals(2, database.list(SINHVIEN).size());
        expectConstraint(() -> database.insert(SINHVIEN,
                values("MASV", "SV03", "MALOP", "MISSING", "HOTEN", "Sinh viên 3")));
        expectConstraint(() -> database.insert(SINHVIEN,
                values("MASV", "sv01", "MALOP", "L01", "HOTEN", "Trùng mã")));
        expectConstraint(() -> database.delete(LOP, "L01"));

        assertTrue(database.update(LOP, "L01", values("TENLOP", "Lớp mới")));
        assertTrue(database.update(SINHVIEN, "SV01", values("HOTEN", "An", "SDT", "0123456789")));
        assertTrue(database.delete(SINHVIEN, "SV02"));
        database.close();
        database = new HssvDatabase(context, TEST_DB);

        assertEquals("Lớp mới", database.list(LOP).get(0).getAsString("TENLOP"));
        ContentValues student = database.list(SINHVIEN).get(0);
        assertEquals("An", student.getAsString("HOTEN"));
        assertEquals("0123456789", student.getAsString("SDT"));
        assertEquals(1, database.list(SINHVIEN).size());
        assertTrue(database.delete(SINHVIEN, "SV01"));
        assertTrue(database.delete(LOP, "L01"));
    }

    @Test
    public void subjectsSupportCrudAndRequirePositiveCredits() {
        database.insert(MONHOC, values("MAMH", "MH01", "TENMH", "Lập trình di động", "SOTINCHI", "3"));
        assertEquals("3", database.list(MONHOC).get(0).getAsString("SOTINCHI"));
        expectConstraint(() -> database.update(MONHOC, "MH01", values("SOTINCHI", "0")));
        expectConstraint(() -> database.update(MONHOC, "MH01", values("SOTINCHI", "abc")));
        assertTrue(database.update(MONHOC, "MH01", values("SOTINCHI", "4")));
        assertEquals("4", database.list(MONHOC).get(0).getAsString("SOTINCHI"));
        assertTrue(database.delete(MONHOC, "MH01"));
        assertTrue(database.list(MONHOC).isEmpty());
    }

    @Test
    public void upgradePreservesVersionOneRecordsAndEnforcesNewRelationships() {
        SQLiteDatabase legacy = database.getWritableDatabase();
        legacy.execSQL("DROP TABLE KETQUA");
        legacy.execSQL("DROP TABLE PHANCONG");
        legacy.execSQL("DROP TABLE GIANGVIEN");
        legacy.setVersion(1);
        database.insert(LOP, values("MALOP", "L01", "TENLOP", "Lớp 1"));
        database.insert(SINHVIEN, values("MASV", "SV01", "MALOP", "L01", "HOTEN", "Sinh viên 1"));
        database.insert(MONHOC, values("MAMH", "MH01", "TENMH", "Lập trình di động", "SOTINCHI", "3"));
        database.close();
        database = new HssvDatabase(context, TEST_DB);

        assertEquals(1, database.count(LOP));
        assertEquals(1, database.count(SINHVIEN));
        assertEquals(1, database.count(MONHOC));
        assertEquals(0, database.count(GIANGVIEN));
        assertEquals(0, database.count(KETQUA));
        assertEquals(0, database.count(PHANCONG));
        assertEquals("Sinh viên 1", database.list(SINHVIEN).get(0).getAsString("HOTEN"));

        database.insert(GIANGVIEN, values("MAGV", "GV01", "HOTEN", "Giảng viên 1"));
        database.insert(PHANCONG, values("MAPC", "PC01", "MAGV", "GV01",
                "MAMH", "MH01", "MALOP", "L01", "HOCKY", "2026-1"));
        database.insert(KETQUA, values("MAKQ", "KQ01", "MASV", "SV01", "MAMH", "MH01", "DIEM", "8.5"));
        assertTrue(database.update(GIANGVIEN, "GV01", values("BOMON", "Công nghệ thông tin")));
        assertTrue(database.update(PHANCONG, "PC01", values("HOCKY", "2026-2")));
        assertTrue(database.update(KETQUA, "KQ01", values("DIEM", "9")));
        assertEquals("2026-2", database.list(PHANCONG).get(0).getAsString("HOCKY"));
        assertEquals(9.0, database.list(KETQUA).get(0).getAsDouble("DIEM"), 0.001);
        expectConstraint(() -> database.update(KETQUA, "KQ01", values("DIEM", "11")));
        expectConstraint(() -> database.insert(KETQUA,
                values("MAKQ", "KQ02", "MASV", "SV01", "MAMH", "MH01", "DIEM", "7")));
        expectConstraint(() -> database.insert(PHANCONG,
                values("MAPC", "PC02", "MAGV", "MISSING", "MAMH", "MH01", "MALOP", "L01", "HOCKY", "2026-1")));
        expectConstraint(() -> database.delete(GIANGVIEN, "GV01"));
        expectConstraint(() -> database.delete(MONHOC, "MH01"));
        assertTrue(database.delete(KETQUA, "KQ01"));
        assertTrue(database.delete(PHANCONG, "PC01"));
        assertTrue(database.delete(GIANGVIEN, "GV01"));
    }

    private static ContentValues values(String... pairs) {
        ContentValues values = new ContentValues();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put(pairs[index], pairs[index + 1]);
        }
        return values;
    }

    private static void expectConstraint(Runnable operation) {
        try {
            operation.run();
            fail("Expected SQLite to reject the operation");
        } catch (SQLiteConstraintException expected) {
            // The operation must fail before the database changes.
        }
    }
}
