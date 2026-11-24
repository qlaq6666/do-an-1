package com.example.n1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.InputType;

import java.util.ArrayList;
import java.util.List;

final class HssvDatabase extends SQLiteOpenHelper {
    static final String NAME = "QuanLyHSSV.db";
    static final int TEXT = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;

    enum Table {
        LOP("Lớp", "TENLOP",
                new Field("MALOP", "Mã lớp", 20, true),
                new Field("TENLOP", "Tên lớp", 100, true),
                new Field("KHOA", "Khoa", 100, false),
                new Field("NIENKHOA", "Niên khóa", 20, false)),
        SINHVIEN("Sinh viên", "HOTEN",
                new Field("MASV", "Mã sinh viên", 20, true),
                new Field("HOTEN", "Họ và tên", 100, true),
                new Field("MALOP", "Lớp", LOP),
                new Field("NGAYSINH", "Ngày sinh (dd/MM/yyyy)", 10, false, InputType.TYPE_CLASS_DATETIME),
                new Field("GIOITINH", "Giới tính", 20, false),
                new Field("SDT", "Số điện thoại", 20, false, InputType.TYPE_CLASS_PHONE),
                new Field("EMAIL", "Email", 150, false,
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
                new Field("DIACHI", "Địa chỉ", 255, false)),
        MONHOC("Môn học", "TENMH",
                new Field("MAMH", "Mã môn học", 20, true),
                new Field("TENMH", "Tên môn học", 150, true),
                new Field("SOTINCHI", "Số tín chỉ", 10, true, InputType.TYPE_CLASS_NUMBER)),
        GIANGVIEN("Giảng viên", "HOTEN",
                new Field("MAGV", "Mã giảng viên", 20, true),
                new Field("HOTEN", "Họ và tên", 100, true),
                new Field("BOMON", "Bộ môn", 100, false),
                new Field("SDT", "Số điện thoại", 20, false, InputType.TYPE_CLASS_PHONE),
                new Field("EMAIL", "Email", 150, false,
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)),
        KETQUA("Kết quả", "MAKQ",
                new Field("MAKQ", "Mã kết quả", 20, true),
                new Field("MASV", "Sinh viên", SINHVIEN),
                new Field("MAMH", "Môn học", MONHOC),
                new Field("DIEM", "Điểm (0–10)", 6, true,
                        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL),
                new Field("GHICHU", "Ghi chú", 255, false)),
        PHANCONG("Phân công giảng dạy", "MAPC",
                new Field("MAPC", "Mã phân công", 20, true),
                new Field("MAGV", "Giảng viên", GIANGVIEN),
                new Field("MAMH", "Môn học", MONHOC),
                new Field("MALOP", "Lớp", LOP),
                new Field("HOCKY", "Học kỳ", 30, true));

        final String label;
        final String displayName;
        final Field[] fields;

        Table(String label, String displayName, Field... fields) {
            this.label = label;
            this.displayName = displayName;
            this.fields = fields;
        }

        String key() {
            return fields[0].name;
        }
    }

    static final class Field {
        final String name;
        final String label;
        final int maxLength;
        final boolean required;
        final int inputType;
        final Table reference;

        Field(String name, String label, int maxLength, boolean required) {
            this(name, label, maxLength, required, TEXT);
        }

        Field(String name, String label, int maxLength, boolean required, int inputType) {
            this(name, label, maxLength, required, inputType, null);
        }

        Field(String name, String label, Table reference) {
            this(name, label, 20, true, TEXT, reference);
        }

        private Field(String name, String label, int maxLength, boolean required, int inputType,
                      Table reference) {
            this.reference = reference;
            this.name = name;
            this.label = label;
            this.maxLength = maxLength;
            this.required = required;
            this.inputType = inputType;
        }
    }

    HssvDatabase(Context context) {
        this(context, NAME);
    }

    HssvDatabase(Context context, String name) {
        super(context, name, null, 2);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE LOP ("
                + "MALOP TEXT COLLATE NOCASE NOT NULL PRIMARY KEY, TENLOP TEXT NOT NULL, "
                + "KHOA TEXT, NIENKHOA TEXT)");
        db.execSQL("CREATE TABLE SINHVIEN ("
                + "MASV TEXT COLLATE NOCASE NOT NULL PRIMARY KEY, MALOP TEXT COLLATE NOCASE NOT NULL, "
                + "HOTEN TEXT NOT NULL, NGAYSINH TEXT, GIOITINH TEXT, SDT TEXT, EMAIL TEXT, DIACHI TEXT, "
                + "FOREIGN KEY (MALOP) REFERENCES LOP (MALOP) ON DELETE RESTRICT)");
        db.execSQL("CREATE INDEX IDX_SINHVIEN_MALOP ON SINHVIEN (MALOP)");
        db.execSQL("CREATE TABLE MONHOC ("
                + "MAMH TEXT COLLATE NOCASE NOT NULL PRIMARY KEY, TENMH TEXT NOT NULL, "
                + "SOTINCHI INTEGER NOT NULL CHECK (typeof(SOTINCHI) = 'integer' AND SOTINCHI > 0))");
        createAdditionalTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            createAdditionalTables(db);
            return;
        }
        throw new IllegalStateException("Thiếu bước nâng cấp CSDL " + oldVersion + " → " + newVersion);
    }

    private void createAdditionalTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE GIANGVIEN ("
                + "MAGV TEXT COLLATE NOCASE NOT NULL PRIMARY KEY, HOTEN TEXT NOT NULL, "
                + "BOMON TEXT, SDT TEXT, EMAIL TEXT)");
        db.execSQL("CREATE TABLE KETQUA ("
                + "MAKQ TEXT COLLATE NOCASE NOT NULL PRIMARY KEY, "
                + "MASV TEXT COLLATE NOCASE NOT NULL REFERENCES SINHVIEN (MASV) ON DELETE RESTRICT, "
                + "MAMH TEXT COLLATE NOCASE NOT NULL REFERENCES MONHOC (MAMH) ON DELETE RESTRICT, "
                + "DIEM REAL NOT NULL CHECK (typeof(DIEM) = 'real' AND DIEM BETWEEN 0 AND 10), "
                + "GHICHU TEXT, UNIQUE (MASV, MAMH))");
        db.execSQL("CREATE INDEX IDX_KETQUA_MAMH ON KETQUA (MAMH)");
        db.execSQL("CREATE TABLE PHANCONG ("
                + "MAPC TEXT COLLATE NOCASE NOT NULL PRIMARY KEY, "
                + "MAGV TEXT COLLATE NOCASE NOT NULL REFERENCES GIANGVIEN (MAGV) ON DELETE RESTRICT, "
                + "MAMH TEXT COLLATE NOCASE NOT NULL REFERENCES MONHOC (MAMH) ON DELETE RESTRICT, "
                + "MALOP TEXT COLLATE NOCASE NOT NULL REFERENCES LOP (MALOP) ON DELETE RESTRICT, "
                + "HOCKY TEXT COLLATE NOCASE NOT NULL, UNIQUE (MAGV, MAMH, MALOP, HOCKY))");
        db.execSQL("CREATE INDEX IDX_PHANCONG_MAMH ON PHANCONG (MAMH)");
        db.execSQL("CREATE INDEX IDX_PHANCONG_MALOP ON PHANCONG (MALOP)");
    }

    List<ContentValues> list(Table table) {
        List<ContentValues> rows = new ArrayList<>();
        String orderBy = table.displayName + " COLLATE NOCASE, " + table.key();
        try (Cursor cursor = getReadableDatabase().query(table.name(), null, null, null,
                null, null, orderBy)) {
            while (cursor.moveToNext()) {
                ContentValues row = new ContentValues();
                for (Field field : table.fields) {
                    row.put(field.name, cursor.getString(cursor.getColumnIndexOrThrow(field.name)));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    long count(Table table) {
        return DatabaseUtils.queryNumEntries(getReadableDatabase(), table.name());
    }

    boolean contains(Table table, String id) {
        try (Cursor cursor = getReadableDatabase().query(table.name(), new String[]{table.key()},
                table.key() + " = ?", new String[]{id}, null, null, null)) {
            return cursor.moveToFirst();
        }
    }

    void insert(Table table, ContentValues values) {
        getWritableDatabase().insertOrThrow(table.name(), null, values);
    }

    boolean update(Table table, String id, ContentValues changes) {
        return getWritableDatabase().update(table.name(), changes,
                table.key() + " = ?", new String[]{id}) == 1;
    }

    boolean delete(Table table, String id) {
        return getWritableDatabase().delete(table.name(),
                table.key() + " = ?", new String[]{id}) == 1;
    }
}
