package com.example.n1;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Qlsv extends ManagementActivity {
    private static final String EXTRA_TABLE = "com.example.n1.TABLE";

    private HssvDatabase database;
    private HssvDatabase.Table table;
    private ListView list;
    private TextView count;

    static Intent intentFor(Context context, HssvDatabase.Table table) {
        return new Intent(context, Qlsv.class).putExtra(EXTRA_TABLE, table.name());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String tableName = getIntent().getStringExtra(EXTRA_TABLE);
        table = tableName == null ? HssvDatabase.Table.SINHVIEN : HssvDatabase.Table.valueOf(tableName);
        database = new HssvDatabase(this);

        showManagementScreen(R.layout.activity_qlsv, table.label);

        count = findViewById(R.id.record_count);
        list = findViewById(R.id.record_list);
        list.setEmptyView(findViewById(R.id.empty_records));
        findViewById(R.id.add_record).setOnClickListener(view -> showEditor(null));
        View assignments = findViewById(R.id.open_assignments);
        if (table == HssvDatabase.Table.GIANGVIEN) {
            assignments.setVisibility(View.VISIBLE);
            assignments.setOnClickListener(view -> startActivity(intentFor(this, HssvDatabase.Table.PHANCONG)));
        }
        refreshList();
    }

    private void refreshList() {
        List<ContentValues> rows = database.list(table);
        List<String> captions = new ArrayList<>();
        for (ContentValues row : rows) {
            captions.add(recordCaption(row));
        }
        count.setText(getString(R.string.record_count, rows.size()));
        list.setAdapter(new ArrayAdapter<>(this, R.layout.hssv_record, captions));
        list.setOnItemClickListener((parent, view, position, id) -> showEditor(rows.get(position)));
    }

    private String recordCaption(ContentValues row) {
        String key = row.getAsString(table.key());
        String caption = row.getAsString(table.displayName) + "\n" + key;
        switch (table) {
            case SINHVIEN:
                return caption + " · Lớp " + row.getAsString("MALOP");
            case MONHOC:
                return caption + " · " + row.getAsString("SOTINCHI") + " tín chỉ";
            case KETQUA:
                return row.getAsString("MASV") + " · " + row.getAsString("MAMH")
                        + "\n" + key + " · Điểm " + row.getAsString("DIEM");
            case PHANCONG:
                return row.getAsString("MAGV") + " · " + row.getAsString("MAMH")
                        + "\n" + key + " · " + row.getAsString("MALOP") + " · " + row.getAsString("HOCKY");
            default:
                return caption;
        }
    }

    private void showEditor(ContentValues existing) {
        boolean editing = existing != null;
        Map<HssvDatabase.Field, List<ContentValues>> choices = new HashMap<>();
        for (HssvDatabase.Field field : table.fields) {
            if (field.reference == null) {
                continue;
            }
            List<ContentValues> options = database.list(field.reference);
            if (options.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.missing_reference, field.label))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.open_reference,
                                (dialog, which) -> startActivity(intentFor(this, field.reference)))
                        .show();
                return;
            }
            choices.put(field, options);
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), dp(12));
        List<View> inputs = new ArrayList<>();
        for (HssvDatabase.Field field : table.fields) {
            View input = makeInput(field, existing, choices.get(field));
            inputs.add(input);
            TextView label = new TextView(this);
            label.setText(field.label + (field.required ? " *" : ""));
            label.setLabelFor(input.getId());
            label.setPadding(0, dp(12), 0, 0);
            form.addView(label);
            form.addView(input, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        if (editing) {
            inputs.get(0).setEnabled(false);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle((editing ? "Sửa " : "Thêm ") + table.label.toLowerCase(Locale.ROOT))
                .setView(scroll)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null);
        if (editing) {
            builder.setNeutralButton(R.string.remove, (dialog, which) -> confirmDelete(existing));
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    if (saveRecord(existing, inputs, choices)) {
                        dialog.dismiss();
                    }
                }));
        dialog.show();
    }

    private View makeInput(HssvDatabase.Field field, ContentValues existing,
                           List<ContentValues> choices) {
        String value = existing == null ? null : existing.getAsString(field.name);
        if (field.reference != null) {
            Spinner spinner = new Spinner(this);
            spinner.setId(View.generateViewId());
            spinner.setMinimumHeight(dp(48));
            List<String> options = new ArrayList<>();
            int selected = 0;
            for (ContentValues row : choices) {
                String code = row.getAsString(field.reference.key());
                if (code.equals(value)) {
                    selected = options.size();
                }
                options.add(code + " · " + row.getAsString(field.reference.displayName));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(selected);
            return spinner;
        }
        EditText edit = new EditText(this);
        edit.setId(View.generateViewId());
        edit.setSingleLine(true);
        edit.setInputType(field.inputType);
        edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(field.maxLength)});
        if ("NGAYSINH".equals(field.name) && value != null) {
            value = formatDate(value, "yyyy-MM-dd", "dd/MM/yyyy");
        }
        edit.setText(value);
        return edit;
    }

    private boolean saveRecord(ContentValues existing, List<View> inputs,
                               Map<HssvDatabase.Field, List<ContentValues>> choices) {
        ContentValues values = new ContentValues();
        for (int index = 0; index < table.fields.length; index++) {
            HssvDatabase.Field field = table.fields[index];
            View input = inputs.get(index);
            if (input instanceof Spinner) {
                int selected = ((Spinner) input).getSelectedItemPosition();
                values.put(field.name, choices.get(field).get(selected).getAsString(field.reference.key()));
            } else if (!readField(values, field, (EditText) input)) {
                return false;
            }
        }

        String id = values.getAsString(table.key());
        try {
            if (existing == null) {
                database.insert(table, values);
            } else {
                values.remove(table.key());
                if (!database.update(table, id, values)) {
                    notifyUser("Bản ghi đã bị xóa.");
                    refreshList();
                    return true;
                }
            }
        } catch (SQLiteConstraintException constraint) {
            if (existing == null && database.contains(table, id)) {
                showError((EditText) inputs.get(0), "Mã này đã tồn tại.");
            } else {
                notifyUser("Dữ liệu bị trùng hoặc bản ghi liên quan đã thay đổi.");
            }
            return false;
        }
        refreshList();
        notifyUser(existing == null ? "Đã thêm bản ghi." : "Đã lưu thay đổi.");
        return true;
    }

    private boolean readField(ContentValues values, HssvDatabase.Field field, EditText input) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            if (field.required) {
                return showError(input, "Vui lòng nhập " + field.label.toLowerCase(Locale.ROOT) + ".");
            }
            values.putNull(field.name);
        } else if ("SOTINCHI".equals(field.name)) {
            try {
                int credits = Integer.parseInt(value);
                if (credits < 1) {
                    return showError(input, "Số tín chỉ phải lớn hơn 0.");
                }
                values.put(field.name, credits);
            } catch (NumberFormatException error) {
                return showError(input, "Số tín chỉ phải là số nguyên dương.");
            }
        } else if ("DIEM".equals(field.name)) {
            try {
                double score = Double.parseDouble(value.replace(',', '.'));
                if (!Double.isFinite(score) || score < 0 || score > 10) {
                    return showError(input, "Điểm phải từ 0 đến 10.");
                }
                values.put(field.name, score);
            } catch (NumberFormatException error) {
                return showError(input, "Vui lòng nhập điểm dạng số.");
            }
        } else if ("NGAYSINH".equals(field.name)) {
            String date = value.matches("[0-9]{2}/[0-9]{2}/[0-9]{4}")
                    ? formatDate(value, "dd/MM/yyyy", "yyyy-MM-dd") : null;
            if (date == null) {
                return showError(input, "Nhập ngày hợp lệ theo dạng dd/MM/yyyy.");
            }
            values.put(field.name, date);
        } else {
            values.put(field.name, value);
        }
        return true;
    }

    private static String formatDate(String value, String from, String to) {
        SimpleDateFormat parser = new SimpleDateFormat(from, Locale.ROOT);
        parser.setLenient(false);
        try {
            Date date = parser.parse(value);
            return date == null ? null : new SimpleDateFormat(to, Locale.ROOT).format(date);
        } catch (ParseException error) {
            return null;
        }
    }

    private boolean showError(EditText input, String message) {
        input.setError(message);
        input.requestFocus();
        return false;
    }

    private void confirmDelete(ContentValues row) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_record)
                .setMessage(recordCaption(row))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    try {
                        boolean removed = database.delete(table, row.getAsString(table.key()));
                        notifyUser(removed ? "Đã xóa bản ghi." : "Bản ghi đã bị xóa.");
                        refreshList();
                    } catch (SQLiteConstraintException constraint) {
                        notifyUser("Bản ghi đang được dùng trong bảng khác. Hãy cập nhật các bản ghi liên quan trước.");
                    }
                })
                .show();
    }

    private void notifyUser(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }
}
