package com.example.botanic_park.PlantSearch;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.botanic_park.*;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import jp.wasabeef.glide.transformations.ColorFilterTransformation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class Fragment_Plant_Book extends Fragment implements AdapterView.OnItemSelectedListener {
    public static final int PERMISSION_REQUEST_CODE = 2000;
    public static final int START_ACTIVITY_CODE = 3000;

    public static final String SELECTED_ITEM_KEY = "selected item";
    public static final String SEARCH_WORD_KEY = "search word";

    MainActivity mainActivity;
    PlantBookExpandableGridView plantBookGridView;
    PlantBookAdapter plantBookAdapter;
    Spinner spinner;
    InputMethodManager inputMethodManager;
    ArrayList<PlantBookItem> list, searchList;
    ProgressBar progressBar;

    public Fragment_Plant_Book() {
    }

    public static Fragment_Plant_Book newInstance() {
        Fragment_Plant_Book fragment = new Fragment_Plant_Book();
        Bundle args = new Bundle();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list = AppManager.getInstance().getList();
        searchList = new ArrayList<>();
        searchList.addAll(list);
        mainActivity = (MainActivity) getActivity();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plant_book, container, false);

        ImageButton cameraSearchBtn = view.findViewById(R.id.camera_search_btn);
        cameraSearchBtn.setOnClickListener(cameraSearchClickListener);

        spinner = view.findViewById(R.id.spinner);
        ArrayAdapter arrayAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.sort_type, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(this);

        plantBookGridView = view.findViewById(R.id.gridview_plant_book);
        plantBookAdapter = new PlantBookAdapter(getContext(),
                R.layout.item_plant, searchList);
        plantBookGridView.setAdapter(plantBookAdapter); // 어댑터를 그리드 뷰에 적용
        plantBookGridView.setOnItemClickListener(itemClickListener);

        final EditText editText = view.findViewById(R.id.search_edit_text);
        editText.addTextChangedListener(textWatcher);

        inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH)
                    inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                return false;
            }
        });

        ImageButton helpButton = view.findViewById(R.id.help_btn);
        helpButton.setOnClickListener(helpClickListener);

        ScrollView scrollView = view.findViewById(R.id.scrollview);
        scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                // [Y축] i1: before i3: after
                if (i1 <= 0 || i3 - i1 > 0) {
                    // top & scroll up
                    mainActivity.changeCurveBottomBarVisibility(true);
                } else if (i3 - i1 < 0) {
                    // scroll down
                    mainActivity.changeCurveBottomBarVisibility(false);
                }

            }
        });

        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setProgress(AppManager.getInstance().collectionCount);
        return view;
    }

    private View.OnClickListener helpClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // 도움말 띄워줌
            Intent intent = new Intent(getContext(), DetailPopUpActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener cameraSearchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String[] permissions = {
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            ArrayList<String> ungranted_permissions = new ArrayList<>();

            for (String permission : permissions) {
                if (!PermissionCheck.isGrantedPermission(getActivity(), permission))
                    ungranted_permissions.add(permission);
            }

            if (ungranted_permissions.size() > 0) {
                // 권한 요청
                PermissionCheck.requestPermissions(getActivity(),
                        ungranted_permissions.toArray(new String[ungranted_permissions.size()]), PERMISSION_REQUEST_CODE);
            } else {
                startCameraActivity();
            }
        }
    };

    private GridView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            // 아이템 클릭시 상세 페이지로 넘어감
            Intent intent = new Intent(getContext(), DetailPopUpActivity.class);
            intent.putExtra(SELECTED_ITEM_KEY, searchList.get(i));
            startActivity(intent);
        }
    };

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String searchword = editable.toString();
            searchPlant(searchword);
        }
    };

    private void searchPlant(String searchword) {
        if (searchword.isEmpty()) {
            searchList.clear();
            searchList.addAll(list);
            sortList(spinner.getSelectedItemPosition());    // 재정렬
        } else {
            getSearchResult(searchword);
        }
        plantBookAdapter.updateAdpater(searchList);
    }

    private void getSearchResult(String searchword) {
        // 텍스트 검색 결과 리스트
        searchList.clear();
        if (searchword.isEmpty())
            return;

        // 단어 분할
        String[] words = searchword.split(" ");
        for (PlantBookItem item : list) {
            for (String word : words) {
                if (item.getName_ko().contains(word)
                        || item.getName_en().contains(word)
                        || item.getName_sc().contains(word)) {
                    searchList.add(item);
                }
            }
        }
    }

    private void startCameraActivity() {
        Intent intent = new Intent(getActivity(), CameraSearchActivity.class);
        startActivityForResult(intent, START_ACTIVITY_CODE);   // 카메라 액티비티 띄움
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 프래그먼트 화면 갱신
        int count = 0;
        for(PlantBookItem item : list){
            if(item.isCollected())
                count++;
        }
        if(AppManager.getInstance().collectionCount != count)
            AppManager.getInstance().collectionCount = count;
        progressBar.setProgress(count);

        for (Fragment fragment : getFragmentManager().getFragments()) {
            if (fragment.isVisible()) {
                final FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.detach(fragment).attach(fragment);
                transaction.commit();
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (PermissionCheck.verifyPermission(grantResults)) {
                // 동의 했을 경우
                startCameraActivity();
            } else {
                // 거부 했을 경우
                Toast.makeText(getContext(), "권한 동의가 필요합니다.", Toast.LENGTH_SHORT).show();

            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void sortList(int position) {
        final int SORT_COLLECTED = 0;
        final int SORT_RECENT = 1;
        final int SORT_NAME = 2;

        switch (position) {
            case SORT_COLLECTED:
                Collections.sort(searchList, new Comparator<PlantBookItem>() {
                    @Override
                    public int compare(PlantBookItem item, PlantBookItem t1) {
                        //Log.d("테스트", item.getId() + " " + t1.getId());
                        return Boolean.compare(!item.isCollected(), !t1.isCollected());
                    }
                });
                break;
            case SORT_RECENT:
                searchList.clear();
                searchList.addAll(list);
                break;
            case SORT_NAME:
                Collections.sort(searchList);
                break;
        }
        plantBookAdapter.updateAdpater(searchList);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        sortList(i);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}

