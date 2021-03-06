package com.example.botanic_park;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.botanic_park.Help.HelpActivity;
import com.example.botanic_park.PlantSearch.DetailPopUpActivity;
import com.example.botanic_park.PlantSearch.Fragment_Plant_Book;
import com.example.botanic_park.PlantSearch.PlantBookItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.smarteist.autoimageslider.IndicatorAnimations;
import com.smarteist.autoimageslider.IndicatorView.draw.controller.DrawController;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderView;
import com.smarteist.autoimageslider.SliderViewAdapter;


import java.lang.reflect.Type;
import java.util.*;

public class Fragment_Home extends Fragment {
    private ArrayList<PlantBookItem> plantsToday;
    Calendar calendar;

    private static final long MIN_CLICK_INTERVAL=600;

    private long mLastClickTime;

    public Fragment_Home() {
    }

    public static Fragment_Home newInstance() {
        Fragment_Home fragment = new Fragment_Home();
        Bundle args = new Bundle();
        fragment.setArguments(args);

        return fragment;
    }

    // 식물 list 저장
    private void onSaveData(ArrayList<PlantBookItem> list) {
        Gson gson = new GsonBuilder().create();
        Type listType = new TypeToken<ArrayList<PlantBookItem>>() {
        }.getType();
        String json = gson.toJson(list, listType);  // arraylist -> json string

        SharedPreferences sp = getActivity().getSharedPreferences("Botanic Park", getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("plant today", json); // JSON으로 변환한 객체를 저장한다.
        editor.commit(); // 완료한다.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TimeZone jst = TimeZone.getTimeZone("Asia/Seoul");
        calendar = Calendar.getInstance(jst);

    }

    @Override
    public void onDestroy() {
        onSaveData(plantsToday); // 오늘의 식물 저장
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        SliderView sliderView = view.findViewById(R.id.imageSlider);
        SliderAdapter sliderAdapter = new SliderAdapter(getContext());
        sliderView.setSliderAdapter(sliderAdapter);

        sliderView.setIndicatorAnimation(IndicatorAnimations.COLOR);
        sliderView.setSliderTransformAnimation(SliderAnimations.FADETRANSFORMATION);
        sliderView.setAutoCycleDirection(SliderView.AUTO_CYCLE_DIRECTION_RIGHT);
        sliderView.startAutoCycle();

        sliderView.setOnIndicatorClickListener(new DrawController.ClickListener() {
            @Override
            public void onIndicatorClicked(int position) {
                sliderView.setCurrentPagePosition(position);
            }
        });

        // 오늘의 식물 선정
        plantsToday = AppManager.getInstance().getPlantsToday();
        setPlantsToday();
        Log.d("onCreateView", "홈화면 갱신");

        GridView gridView = view.findViewById(R.id.gridview_plant_today);
        TextView textView = view.findViewById(R.id.title_plants_today);
        if (isPlantsTodayComplete()) {
            // 바코드 보여줌
            gridView.setVisibility(View.INVISIBLE);
            textView.setText("오늘의 쿠폰");
            LinearLayout linearLayout = view.findViewById(R.id.barcode);
            linearLayout.setVisibility(View.VISIBLE);

        } else {
            // 오늘의 식물 보여줌
            gridView.setVisibility(View.VISIBLE);
            textView.setText("오늘의 식물");
            PlantTodayAdapter plantTodayAdapter = new PlantTodayAdapter(getContext(),
                    R.layout.item_plant_today, plantsToday);
            gridView.setAdapter(plantTodayAdapter);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    long currentClickTime= SystemClock.uptimeMillis();
                    long elapsedTime=currentClickTime-mLastClickTime;
                    mLastClickTime=currentClickTime;

                    // 중복 클릭인 경우
                    if(elapsedTime<=MIN_CLICK_INTERVAL){
                        return;
                    }

                    Intent intent = new Intent(getContext(), DetailPopUpActivity.class);
                    intent.putExtra(Fragment_Plant_Book.SELECTED_ITEM_KEY, plantsToday.get(i));
                    startActivity(intent);
                }
            });
        }

        ImageButton helpBtn = view.findViewById(R.id.help_btn);
        helpBtn.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(getContext(), HelpActivity.class);
                intent.putExtra(HelpActivity.HELP_CODE, HelpActivity.HELP_TODAY_PLANT);
                startActivity(intent);
            }
        });

        return view;
    }

    private void setPlantsToday() {
        // 오늘의 식물 구하는 공식
        // 오늘 날짜에 * 240 곱해서 119로 나눈 나머지를 시작 인덱스로 3개 뽑음
        Log.d("오늘의 식물", "setPlantsToday");
        int index = (240 * getDate()) % 119;

        ArrayList<PlantBookItem> list = AppManager.getInstance().getList();
        ArrayList<PlantBookItem>  nowPlantsToday = new ArrayList<>();
        nowPlantsToday.add(getNewItem(list.get(index + 1)));
        nowPlantsToday.add(getNewItem(list.get(index + 2)));
        nowPlantsToday.add(getNewItem(list.get(index + 3)));

        if(plantsToday == null ||
                (plantsToday != null && !isSamePlantList(plantsToday, nowPlantsToday))){
            // 오늘의 식물이 갱신됨
            plantsToday = nowPlantsToday;
            AppManager.getInstance().setPlantsToday(plantsToday);
            Toast.makeText(getActivity(), "오늘의 식물이 갱신되었습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isSamePlantList(ArrayList<PlantBookItem> list1, ArrayList<PlantBookItem> list2){
        for(int i=0; i<list1.size(); i++){
            String name1 = list1.get(i).getName_ko();
            String name2 =list2.get(i).getName_ko();
            if(!name1.equals(name2))
                return false;   // 하나라도 다르면 다른 날짜
        }
        return true;
    }

    private int getDate() {
        // 오늘 날짜 정보를 숫자로 반환
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        Log.d("날짜", month + " " + day);

        return 100 * month + day;
    }

    private boolean isPlantsTodayComplete() {
        for (PlantBookItem item : plantsToday) {
            if (!item.isCollected())
                return false;
        }
        return true;
    }

    private PlantBookItem getNewItem(PlantBookItem item) {
        return new PlantBookItem(item.getId(), item.getImg_url(),
                item.getName_ko(), item.getName_sc(), item.getName_en(),
                item.getType(), item.getBlossom(), item.getDetails());
    }

}

class SliderAdapter extends SliderViewAdapter<SliderAdapter.ViewHolder> {
    private Context context;

    public SliderAdapter(Context context) {
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_slider, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        switch (position) {
            case 0:
                Glide.with(viewHolder.itemView)
                        .load(R.drawable.home_2)
                        .centerCrop()
                        .into(viewHolder.imageView);
                break;
            case 1:
                Glide.with(viewHolder.itemView)
                        .load(R.drawable.home_4)
                        .centerCrop()
                        .into(viewHolder.imageView);
                break;
            case 2:
                Glide.with(viewHolder.itemView)
                        .load(R.drawable.home_5)
                        .centerCrop()
                        .into(viewHolder.imageView);
                break;
        }

    }

    @Override
    public int getCount() {
        return 3;
    }

    class ViewHolder extends SliderViewAdapter.ViewHolder {
        View itemView;
        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_home_background);
            this.itemView = itemView;
        }
    }
}

class PlantTodayAdapter extends BaseAdapter {
    private ArrayList<PlantBookItem> itemList;
    private Context context;

    int layout;
    LayoutInflater layoutInflater;

    public PlantTodayAdapter(Context context, int layout, ArrayList<PlantBookItem> itemList) {
        this.itemList = itemList;
        this.layout = layout;
        this.context = context;

        layoutInflater = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int i) {
        return itemList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null)
            view = layoutInflater.inflate(layout, null);

        PlantBookItem item = itemList.get(i);
        TextView textView = view.findViewById(R.id.name);
        textView.setText(item.getName_ko());

        if (item.isCollected()) {
            view.setBackground(ContextCompat.getDrawable(context, R.drawable.border_oval_active));
            ImageView imageView = view.findViewById(R.id.icon);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
            layoutParams.setMargins(5, 5, 5, 5);
            imageView.setImageDrawable(view.getResources().getDrawable(R.drawable.ic_lotus_active));
            textView.setTextColor(view.getResources().getColor(R.color.colorBase));
        }

        return view;
    }

}


