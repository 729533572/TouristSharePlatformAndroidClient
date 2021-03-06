package zjut.salu.share.fragment.banggumi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.zhy.view.flowlayout.FlowLayout;
import com.zhy.view.flowlayout.TagAdapter;
import com.zhy.view.flowlayout.TagFlowLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import zjut.salu.share.R;
import zjut.salu.share.activity.LoginActivity;
import zjut.salu.share.activity.banggumi.BanggumeActivity;
import zjut.salu.share.activity.banggumi.BanggumiDetailActivity;
import zjut.salu.share.activity.user.DownloadActivity;
import zjut.salu.share.adapter.lightstrategy.BanggumeListRecycleAdapter;
import zjut.salu.share.config.CuteTouristShareConfig;
import zjut.salu.share.event.MyRecycleViewScrollListener;
import zjut.salu.share.fragment.RxLazyFragment;
import zjut.salu.share.model.lightstrategy.banggume.Banggume;
import zjut.salu.share.model.lightstrategy.banggume.BanggumeTag;
import zjut.salu.share.utils.ConstantUtil;
import zjut.salu.share.utils.LogUtil;
import zjut.salu.share.utils.NumberUtil;
import zjut.salu.share.utils.OkHttpUtils;
import zjut.salu.share.utils.PreferenceUtils;
import zjut.salu.share.utils.RequestURLs;
import zjut.salu.share.utils.ToastUtils;
import zjut.salu.share.widget.UserTagView;

/**视频详情fragment
 * Created by Salu on 2017/1/29.
 */

public class BanggumeIndroductionFragment extends RxLazyFragment{
    @Bind(R.id.tv_title) TextView mTitleText;
    @Bind(R.id.tv_play_time) TextView mPlayTimeText;
    @Bind(R.id.tv_review_count) TextView mReviewCountText;
    @Bind(R.id.tv_description) TextView mDescText;
    @Bind(R.id.author_tag) UserTagView mAuthorTagView;
    @Bind(R.id.share_num) TextView mShareNum;
    @Bind(R.id.fav_num) TextView mFavNum;
    @Bind(R.id.tags_layout) TagFlowLayout mTagFlowLayout;
    @Bind(R.id.recycle) RecyclerView mRecyclerView;
    @Bind(R.id.layout_video_related) RelativeLayout mVideoRelatedLayout;
    @Bind(R.id.iv_loading_failed_friend_focus)ImageView loadingFailedIV;
    @Bind(R.id.iv_empty_friend_focus)ImageView emptyIV;

    @Bind(R.id.ib_collect)ImageButton collectBtn;
    @Bind(R.id.tv_collect)TextView collectTV;

    private String av;
    private Banggume banggume;
    private List<Banggume> relateBanggumeList;
    private Context context;
    private OkHttpUtils okHttpUtils;
    private BanggumeListRecycleAdapter adapter;
    private WeakReference<RecyclerView> recyclerReference;
    private ImageLoader imageLoader;

    private Boolean focusStatus=false;

    public static BanggumeIndroductionFragment newInstance(String aid, Banggume banggume)
    {

        BanggumeIndroductionFragment fragment = new BanggumeIndroductionFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ConstantUtil.BANGGUMI_TITLE, aid);
        bundle.putSerializable("banggume",banggume);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public int getLayoutResId()
    {

        return R.layout.fragment_banggume_introduction;
    }

    @Override
    public void finishCreateView(Bundle state)
    {
        context=getActivity();
        recyclerReference=new WeakReference<>(mRecyclerView);
        imageLoader=ImageLoader.getInstance();
        okHttpUtils=new OkHttpUtils();
        av = getArguments().getString(ConstantUtil.BANGGUMI_TITLE);
        banggume= (Banggume) getArguments().getSerializable("banggume");
        loadData();
    }

    /**
     * 缓存按钮
     */
    @OnClick(R.id.ib_download)
    public void downloadClick(View v){
        DownloadActivity.launch((Activity) context,banggume);
    }

    /**
     * 检查收藏情况
     */
    private void checkCollectStatus() {
        Map<String,Object> params=new HashMap<>();
        params.put("entityid",banggume.getBangumeid());
        params.put("userid",PreferenceUtils.getString("userid",null));
        Observable<String> observable=okHttpUtils.asyncGetRequest(RequestURLs.CHECK_COLLECT_STATUS,params);
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {}
                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(String result) {
                        if(result.equals("yes")){
                            focusStatus=true;
                            collectBtn.setImageDrawable(CuteTouristShareConfig.mInstance.getResources().getDrawable(R.drawable.ic_uncollect));
                            collectTV.setText(R.string.has_collect_text);
                        }else{
                            focusStatus=false;
                            collectBtn.setImageDrawable(CuteTouristShareConfig.mInstance.getResources().getDrawable(R.drawable.ic_collect));
                            collectTV.setText(R.string.collect_text);
                        }
                    }
                });
    }


    @Override
    protected void loadData()
    {
        //TODO:联网获取额外数据
        if(PreferenceUtils.getBoolean("loginStatus",false)){
            checkCollectStatus();
        }
        finishTask();
    }

    @Override
    protected void finishTask()
    {

        //设置视频标题
        mTitleText.setText(av);
        //设置视频播放数量
        mPlayTimeText.setText(NumberUtil.converString(banggume.getClicknum()));
        //设置视频弹幕数量
        mReviewCountText.setText(NumberUtil.converString(1234));//TODO:评论数
        //设置Up主信息
        mDescText.setText(banggume.getBangumecontent());
        mAuthorTagView.setUpWithInfo(getActivity(),
                banggume.getUser().getUsername(),
                RequestURLs.MAIN_URL+banggume.getUser().getHeaderimage(),banggume.getUser().getUserid());
        //设置分享 收藏 投币数量
        mShareNum.setText(NumberUtil.converString(banggume.getBanggumesharenum()));
        mFavNum.setText(NumberUtil.converString(banggume.getBanggumecollectnum()));
        //设置视频tags
        setVideoTags();
        //设置视频相关
        setVideoRelated();
    }

    /**
     * 收藏按钮
     */
    @OnClick(R.id.ib_collect)
    public void collectClick(View v){
        if(!PreferenceUtils.getBoolean("loginStatus",false)){
            ToastUtils.ShortToast(R.string.please_login_first_text);
            Intent intent=new Intent(context,LoginActivity.class);
            intent.putExtra("activity_name",context.getClass().getName());
            startActivity(intent);
            return;
        }
        Map<String,Object> params=new HashMap<>();
        params.put("entityid",banggume.getBangumeid());
        params.put("userid",PreferenceUtils.getString("userid",null));
        params.put("type","banggume");
        Observable<String> observable=null;
        if(focusStatus){//已关注,则取消关注
            observable=okHttpUtils.asyncGetRequest(RequestURLs.CANCEL_FAVORITE,params);
        }else{//未关注,则关注
            observable=okHttpUtils.asyncGetRequest(RequestURLs.ADD_FAVORITE,params);
        }
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(String result) {
                        if(result.equals("success")){
                            if(focusStatus){//之前收藏,现在已经取消
                                focusStatus=false;
                                collectBtn.setImageResource(R.drawable.ic_collect);
                                collectTV.setText(R.string.collect_text);
                            }else{
                                focusStatus=true;
                                collectBtn.setImageResource(R.drawable.ic_uncollect);
                                collectTV.setText(R.string.has_collect_text);
                            }
                        }else{
                         ToastUtils.ShortToast(R.string.server_down_text);
                        }
                    }
                });

    }

    /**
     * 缓存按钮点击事件
     */
    @OnClick(R.id.ib_download)
    public void downloadBanggumeClick(View v){
        
    }

    /**
     * 加载相关小视频
     */
    private void setVideoRelated()
    {
        Gson gson=new Gson();
        List<Map<String,Object>> params=new ArrayList<>();
        Map<String,Object> map=new HashMap<>();
        map.put("banggumeId",banggume.getBangumeid());
        map.put("tags",banggume.getBangumetags());
        map.put("userid",banggume.getUser().getUserid());
        params.add(map);
        Observable<String> observable=okHttpUtils.asyncPostRequest(params,RequestURLs.LOAD_RELATE_BANGGUME);
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        ((Activity)context).runOnUiThread(()->{
                            loadingFailedIV.setVisibility(View.INVISIBLE);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        ((Activity)context).runOnUiThread(()->{
                            ToastUtils.ShortToast(R.string.server_down_text);
                            loadingFailedIV.setVisibility(View.VISIBLE);
                            emptyIV.setVisibility(View.INVISIBLE);
                        });
                    }

                    @Override
                    public void onNext(String result) {
                        Gson gson=new Gson();
                        relateBanggumeList=gson.fromJson(result,new TypeToken<List<Banggume>>(){}.getType());
                        adapter=new BanggumeListRecycleAdapter(recyclerReference.get(),relateBanggumeList,imageLoader);
                        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                        mRecyclerView.setAdapter(adapter);
                        adapter.setOnItemClickListener((position, holder) -> {
                            Banggume banggume=relateBanggumeList.get(position);
                            BanggumiDetailActivity.launch((Activity) context,banggume);
                        });
                        mRecyclerView.addOnScrollListener(new MyRecycleViewScrollListener(null,null));
                        if(relateBanggumeList.size()==0){
                            emptyIV.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    /**
     * 加载视频tag
     */
    private void setVideoTags()
    {
        List<BanggumeTag> tags=banggume.getBanggimeTagList();
        mTagFlowLayout.setAdapter(new TagAdapter<BanggumeTag>(tags) {
            @Override
            public View getView(FlowLayout parent, int position, BanggumeTag banggumeTag) {
                TextView mTags = (TextView) LayoutInflater.from(getActivity())
                        .inflate(R.layout.layout_tags_item, parent, false);
                mTags.setText(banggumeTag.getBanggumetagname());
                return mTags;
            }
        });
        mTagFlowLayout.setOnSelectListener(selectPosSet -> {
            Iterator<Integer> iterator=selectPosSet.iterator();
            while(iterator.hasNext()){
                BanggumeActivity.launch((Activity) context,tags.get(iterator.next()),"","");
            }
        });
    }

    @OnClick(R.id.btn_share)
    public void share()
    {
        //TODO:分享按钮
//        Intent intent = new Intent(Intent.ACTION_SEND);
//        intent.setType("text/plain");
//        intent.putExtra(Intent.EXTRA_SUBJECT, "分享");
//        intent.putExtra(Intent.EXTRA_TEXT, "来自「哔哩哔哩」的分享:" + mVideoDetailsInfo.getDesc());
//        startActivity(Intent.createChooser(intent, mVideoDetailsInfo.getTitle()));
    }

}
