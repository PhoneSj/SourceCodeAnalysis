## FrameLayout源码分析
---
#### onMeasure()测量方法

<pre><code>
@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	//获得子视图的数量
        int count = getChildCount();
		//宽高给出的不是精确值
        final boolean measureMatchParentChildren =
                MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
        mMatchParentChildren.clear();
		//子控件中占用的最大宽高值(包括子控件的marging值)
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;
		//查找所有视图中占用的最大宽度、高度
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (mMeasureAllChildren || child.getVisibility() != GONE) {
				//测量子控件
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
				//获取在第一次测量后的实际宽高
                maxWidth = Math.max(maxWidth,
                        child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                maxHeight = Math.max(maxHeight,
                        child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                childState = combineMeasuredStates(childState, child.getMeasuredState());
				//当前控件给出的宽高不是精确值，同时子控件中有匹配父控件的测量模式存在，将这些子控件加入到集合中，后面需要重新测量
                if (measureMatchParentChildren) {
                    if (lp.width == LayoutParams.MATCH_PARENT ||
                            lp.height == LayoutParams.MATCH_PARENT) {
                        mMatchParentChildren.add(child);
                    }
                }
            }
        }

        // Account for padding too
        //加上FrameLayout控件的padding值(该值可能受前景图的影响)
        maxWidth += getPaddingLeftWithForeground() + getPaddingRightWithForeground();
        maxHeight += getPaddingTopWithForeground() + getPaddingBottomWithForeground();

        // Check against our minimum height and width
        //与背景图片的宽高比较，取大值
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // Check against our foreground's minimum height and width
        //与前景图片的宽高比较，取大值
        final Drawable drawable = getForeground();
        if (drawable != null) {
            maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
            maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
        }
		//设置测量尺寸，使生效--此时FrameLayout的尺寸大小确定
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));

		//当该控件为warp_content时，并且有子控件为match_parent时，这些子控件需要测量第二次
		//(因为第一次测量时给的尺寸为默认值，这里需要将默认值再次修改为匹配父控件尺寸)
        count = mMatchParentChildren.size();
        if (count > 1) {
            for (int i = 0; i < count; i++) {
                final View child = mMatchParentChildren.get(i);
                final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                final int childWidthMeasureSpec;
				//第二次测量考虑了前景图的padding值，第一次测量没有考虑前景图的padding值
                if (lp.width == LayoutParams.MATCH_PARENT) {
					//当子控件高度为match_parent时，子控件使用父控件的高度(出去padding、margin值)
                    final int width = Math.max(0, getMeasuredWidth()
                            - getPaddingLeftWithForeground() - getPaddingRightWithForeground()
                            - lp.leftMargin - lp.rightMargin);
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            width, MeasureSpec.EXACTLY);
                } else {
                	//考虑前景图的padding值重新测量依次宽度
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            getPaddingLeftWithForeground() + getPaddingRightWithForeground() +
                            lp.leftMargin + lp.rightMargin,
                            lp.width);
                }

                final int childHeightMeasureSpec;
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    final int height = Math.max(0, getMeasuredHeight()
                            - getPaddingTopWithForeground() - getPaddingBottomWithForeground()
                            - lp.topMargin - lp.bottomMargin);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            height, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                            getPaddingTopWithForeground() + getPaddingBottomWithForeground() +
                            lp.topMargin + lp.bottomMargin,
                            lp.height);
                }
				//再次测量子控件
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }
</code></pre>

* 在第一个for循环中：
* - 依次测量所有的占据空间子控件；
* - 比较这些子控件的宽高，得出最大值maxWidth、maxHeight，这两个值为FrameLayout实现对子控件布局所需的宽高；
* - 将那些宽高为match_parent的子控件加入到集合mMatchParentChildren中，后续需要对这些子控件进行二次测量。

* 在第二个for循环之前，第二个for循环之后：
* - maxWidth/maxHeight加入上FrameLayout的padding值（这里包括前景图的padding值）
* - maxWidth/maxHeight与背景图比较，得出较大值
* - maxWidth/maxHeight与前景图比较，得出较大值
* - 将maxWidth/maxHeight与FrameLayout的参数widthMeasureSpec/heightMeasureSpec进行综合比较，最终确定FrameLayout的大小尺寸，并调用setMeasuredDimension()方法使该尺寸生效。

* 在第二个for循环中：确定宽高为match_parent的子控件的大小
* - FrameLayout的宽高减去FrameLayout的padding值和子控件的margin值即为子控件的宽高


#### 综上：
FrameLayout中设置为match_parent的可见子控件会测量两次，而其他可见子控件仅测量一次。


### onLayout()布局方法

<pre><code>
@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutChildren(left, top, right, bottom, false /* no force left gravity */);
    }

    void layoutChildren(int left, int top, int right, int bottom,
                                  boolean forceLeftGravity) {
        final int count = getChildCount();
		//子控件显示在该控件上的x方向的范围
        final int parentLeft = getPaddingLeftWithForeground();
        final int parentRight = right - left - getPaddingRightWithForeground();
		//子控件显示在该控件上的y方向的范围
        final int parentTop = getPaddingTopWithForeground();
        final int parentBottom = bottom - top - getPaddingBottomWithForeground();
		//循环遍历可见的子控件，并对其布局
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
			//只对可见控件布局
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = DEFAULT_CHILD_GRAVITY;
                }

                final int layoutDirection = getLayoutDirection();//系统的布局方式:LTR,RTL
                final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);//水平方向对齐
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;//竖直方向对齐
				//水平方向对齐方式判定，默认左对齐。计算出子控件的左边界坐标
                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
                        lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        if (!forceLeftGravity) {
                            childLeft = parentRight - width - lp.rightMargin;
                            break;
                        }
                    case Gravity.LEFT:
                    default:
                        childLeft = parentLeft + lp.leftMargin;
                }
				//竖直方向对齐方式判定，默认顶部对齐。计算出子控件的上边界坐标
                switch (verticalGravity) {
                    case Gravity.TOP:
                        childTop = parentTop + lp.topMargin;
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = parentTop + (parentBottom - parentTop - height) / 2 +
                        lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = parentBottom - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = parentTop + lp.topMargin;
                }
				//对子控件布局
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }
        }
    }
</code></pre>

* 根据系统的布局方向layoutDirection、水平对齐absoluteGravity、竖直对齐verticalGravity来计算子控件的左边界childLeft和上边界childTop的值。最后得出右边界、下边界，确定子控件的位置。
* 

**总结**
* 若FrameLayout测量模式为精确模式，那么所有的子控件只会测量一次
* 若FrameLayout测量模式为至多模式，那么设置为match_parent的子控件会测量两次