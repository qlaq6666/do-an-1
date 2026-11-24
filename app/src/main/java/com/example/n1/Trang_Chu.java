package com.example.n1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

public class Trang_Chu extends HssvActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        useDarkSystemBars();
        setContentView(R.layout.activity_trang_chu);
        applyScreenInsets(findViewById(R.id.main), false);

        LinearLayout tiles = findViewById(R.id.dashboard_tiles);
        addTableTile(tiles, R.id.btnSV, R.string.manage_students, R.drawable.ic_dashboard_students,
                HssvDatabase.Table.SINHVIEN);
        addTableTile(tiles, R.id.btnLop, R.string.manage_classes, R.drawable.ic_dashboard_classes,
                HssvDatabase.Table.LOP);
        addTableTile(tiles, R.id.btnMonHoc, R.string.manage_subjects, R.drawable.ic_dashboard_subjects,
                HssvDatabase.Table.MONHOC);
        addTableTile(tiles, R.id.btnGiangVien, R.string.manage_teachers, R.drawable.ic_dashboard_teachers,
                HssvDatabase.Table.GIANGVIEN);
        addTableTile(tiles, R.id.btnKetQua, R.string.manage_results, R.drawable.ic_dashboard_results,
                HssvDatabase.Table.KETQUA);
        addTile(tiles, R.id.btnThongKe, R.string.dashboard_stats, R.drawable.ic_dashboard_stats,
                this::showStatistics);

        findViewById(R.id.btnDX).setOnClickListener(view -> {
            startActivity(new Intent(this, Login.class));
            finish();
        });
    }

    private void addTableTile(LinearLayout tiles, int id, int label, int icon, HssvDatabase.Table table) {
        addTile(tiles, id, label, icon, () -> startActivity(Qlsv.intentFor(this, table)));
    }

    private void addTile(LinearLayout tiles, int id, int label, int icon, Runnable action) {
        View tile = getLayoutInflater().inflate(R.layout.dashboard_tile, tiles, false);
        tile.setId(id);
        ((ImageView) tile.findViewById(R.id.tile_icon)).setImageResource(icon);
        ((TextView) tile.findViewById(R.id.tile_title)).setText(label);
        tile.setContentDescription(getString(label));
        tile.setFocusable(true);
        tile.setOnClickListener(view -> action.run());
        tiles.addView(tile);
    }

    private void showStatistics() {
        String summary;
        try (HssvDatabase database = new HssvDatabase(this)) {
            summary = getString(R.string.dashboard_stats_summary,
                    database.count(HssvDatabase.Table.SINHVIEN),
                    database.count(HssvDatabase.Table.LOP),
                    database.count(HssvDatabase.Table.MONHOC),
                    database.count(HssvDatabase.Table.GIANGVIEN),
                    database.count(HssvDatabase.Table.KETQUA),
                    database.count(HssvDatabase.Table.PHANCONG));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.dashboard_stats)
                .setMessage(summary)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
