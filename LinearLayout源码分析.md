## LinearLayout源码分析

### onMeasure()测量方法

```
@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	//子控件在父控件排布的方向，默认水平方向
        if (mOrientation == VERTICAL) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

```

* 该方法中根据当前LinearLayout的方向决定布局方向，根据布局方向决定测量
* - measureVertical()竖直测量
* - measureHorizontal()水平测量


因两个方法计算都一样，下面只分析竖直方向的测量

```
void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
    	//已经使用的高度总和
        mTotalLength = 0;
        int maxWidth = 0;
        int childState = 0;
        int alternativeMaxWidth = 0;
        int weightedMaxWidth = 0;
        boolean allFillParent = true;
		//子控件权重值总和
        float totalWeight = 0;

        final int count = getVirtualChildCount();
        
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        boolean matchWidth = false;
        boolean skippedMeasure = false;//是否跳过本次测量

        final int baselineChildIndex = mBaselineAlignedChildIndex;        
        final boolean useLargestChild = mUseLargestChild;

        int largestChildHeight = Integer.MIN_VALUE;

        // See how tall everyone is. Also remember max width.
        for (int i = 0; i < count; ++i) {
            final View child = getVirtualChildAt(i);
			//剔除空元素
            if (child == null) {
                mTotalLength += measureNullChild(i);
                continue;
            }
			//剔除不占空间的元素
            if (child.getVisibility() == View.GONE) {
               i += getChildrenSkipCount(child, i);
               continue;
            }
			//加上分割线的高度
            if (hasDividerBeforeChildAt(i)) {
                mTotalLength += mDividerHeight;
            }

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
			//计入权重累加
            totalWeight += lp.weight;
            
            if (heightMode == MeasureSpec.EXACTLY && lp.height == 0 && lp.weight > 0) {
                // Optimization: don't bother measuring children who are going to use
                // leftover space. These views will get measured again down below if
                // there is any leftover space.
                //当LinearLayout为EXACTLY，且子控件高度为0，权重大于0时，该子控件需要待所有控件测量完成才知道剩余的高度，
                //所以在本次循环中不测量，在第二次循环中测量
                final int totalLength = mTotalLength;
				//这里提前将该子控件的margin值加入总高度
                mTotalLength = Math.max(totalLength, totalLength + lp.topMargin + lp.bottomMargin);
				//标记，表示在本次遍历子控件时不测量，跳过，在下次遍历子控件时在测量
                skippedMeasure = true;
            } else {
                int oldHeight = Integer.MIN_VALUE;
				//子控件高度为0时，高度改为WARP_CONTENT，并使用oldHeight记住旧高度值，后面测量完该子控件后需要恢复旧高度值
                if (lp.height == 0 && lp.weight > 0) {
                    // heightMode is either UNSPECIFIED or AT_MOST, and this
                    // child wanted to stretch to fill available space.
                    // Translate that to WRAP_CONTENT so that it does not end up
                    // with a height of 0
                    //
                    //当前控件测量模式为unspecified或者AT_MOST时，同时当前子控件希望填充父控件可用的空闲区域，将测量模式转换成warp_content，
                    //以避免当前子控件高度被设置为0(高度需要被设置成默认值)
                    oldHeight = 0;
                    lp.height = LayoutParams.WRAP_CONTENT;
                }

                // Determine how big this child would like to be. If this or
                // previous children have given a weight, then we allow it to
                // use all available space (and we will shrink things later
                // if needed).
                //测量子控件，当权重总和不为0，那么总消耗高度为0，这时测量子控件不考虑前面子控件测量消耗的高度
                measureChildBeforeLayout(
                       child, i, widthMeasureSpec, 0, heightMeasureSpec,
                       totalWeight == 0 ? mTotalLength : 0);
				//该子控件恢复旧高度值
                if (oldHeight != Integer.MIN_VALUE) {
                   lp.height = oldHeight;
                }

                final int childHeight = child.getMeasuredHeight();
                final int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + childHeight + lp.topMargin +
                       lp.bottomMargin + getNextLocationOffset(child));

                if (useLargestChild) {
                    largestChildHeight = Math.max(childHeight, largestChildHeight);
                }
            }

            /**
             * If applicable, compute the additional offset to the child's baseline
             * we'll need later when asked {@link #getBaseline}.
             */
            if ((baselineChildIndex >= 0) && (baselineChildIndex == i + 1)) {
               mBaselineChildTop = mTotalLength;
            }

            // if we are trying to use a child index for our baseline, the above
            // book keeping only works if there are no children above it with
            // weight.  fail fast to aid the developer.
            if (i < baselineChildIndex && lp.weight > 0) {
                throw new RuntimeException("A child of LinearLayout with index "
                        + "less than mBaselineAlignedChildIndex has weight > 0, which "
                        + "won't work.  Either remove the weight, or don't set "
                        + "mBaselineAlignedChildIndex.");
            }

            boolean matchWidthLocally = false;
            if (widthMode != MeasureSpec.EXACTLY && lp.width == LayoutParams.MATCH_PARENT) {
                // The width of the linear layout will scale, and at least one
                // child said it wanted to match our width. Set a flag
                // indicating that we need to remeasure at least that view when
                // we know our width.
                matchWidth = true;
                matchWidthLocally = true;
            }

            final int margin = lp.leftMargin + lp.rightMargin;
            final int measuredWidth = child.getMeasuredWidth() + margin;
            maxWidth = Math.max(maxWidth, measuredWidth);
            childState = combineMeasuredStates(childState, child.getMeasuredState());

            allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT;
            if (lp.weight > 0) {
                /*
                 * Widths of weighted Views are bogus if we end up
                 * remeasuring, so keep them separate.
                 */
                weightedMaxWidth = Math.max(weightedMaxWidth,
                        matchWidthLocally ? margin : measuredWidth);
            } else {
                alternativeMaxWidth = Math.max(alternativeMaxWidth,
                        matchWidthLocally ? margin : measuredWidth);
            }

            i += getChildrenSkipCount(child, i);
        }

		//最后一条分割线(最后一个子控件的下面的end分割线)
        if (mTotalLength > 0 && hasDividerBeforeChildAt(count)) {
            mTotalLength += mDividerHeight;
        }

        if (useLargestChild &&
                (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED)) {
            mTotalLength = 0;

            for (int i = 0; i < count; ++i) {
                final View child = getVirtualChildAt(i);

                if (child == null) {
                    mTotalLength += measureNullChild(i);
                    continue;
                }

                if (child.getVisibility() == GONE) {
                    i += getChildrenSkipCount(child, i);
                    continue;
                }

                final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)
                        child.getLayoutParams();
                // Account for negative margins
                final int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + largestChildHeight +
                        lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));
            }
        }

        // Add in our padding
        //已使用的总高度加上padding值
        mTotalLength += mPaddingTop + mPaddingBottom;

        int heightSize = mTotalLength;

        // Check against our minimum height
        //和最小高度(背景图片高度)比较取较大值
        heightSize = Math.max(heightSize, getSuggestedMinimumHeight());
        
        // Reconcile our calculated size with the heightMeasureSpec
        //将通过子控件计算的高度与父控件建议的高度总和考虑，得出最终高度值
        int heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0);
        heightSize = heightSizeAndState & MEASURED_SIZE_MASK;
        
        // Either expand children with weight to take up available space or
        // shrink them if they extend beyond our current bounds. If we skipped
        // measurement on any children, we need to measure them now.
        //剩余高度(减去消耗高度后的剩余高度值)
        int delta = heightSize - mTotalLength;
		//针对权重对子控件二次测量
        if (skippedMeasure || delta != 0 && totalWeight > 0.0f) {
			//LinearLaout的权重总和(本身设置了权重且大于0，就使用LinearLayout的自身的，
			//否则使用子控件权重累加值)
            float weightSum = mWeightSum > 0.0f ? mWeightSum : totalWeight;

            mTotalLength = 0;

            for (int i = 0; i < count; ++i) {
                final View child = getVirtualChildAt(i);
                //忽略gone子控件
                if (child.getVisibility() == View.GONE) {
                    continue;
                }
                
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                //子控件权重
                float childExtra = lp.weight;
                if (childExtra > 0) {
                    // Child said it could absorb extra space -- give him his share
                    //子控件从父控件中分得的剩余高度值
                    int share = (int) (childExtra * delta / weightSum);
                    weightSum -= childExtra;
                    delta -= share;

                    final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            mPaddingLeft + mPaddingRight +
                                    lp.leftMargin + lp.rightMargin, lp.width);

                    // TODO: Use a field like lp.isMeasured to figure out if this
                    // child has been previously measured
                    if ((lp.height != 0) || (heightMode != MeasureSpec.EXACTLY)) {
                        // child was measured once already above...
                        // base new measurement on stored values
                        int childHeight = child.getMeasuredHeight() + share;
                        if (childHeight < 0) {
                            childHeight = 0;
                        }
                        //子控件加上新分得的剩余高度进行二次测量
                        child.measure(childWidthMeasureSpec,
                                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
                    } else {
                        // child was skipped in the loop above.
                        // Measure for this first time here   
                        //在xml中layout_height=0的子控件，上一轮没有进行测量，这次对子控件进行第一次测量
                        child.measure(childWidthMeasureSpec,
                                MeasureSpec.makeMeasureSpec(share > 0 ? share : 0,
                                        MeasureSpec.EXACTLY));
                    }

                    // Child may now not fit in vertical dimension.
                    childState = combineMeasuredStates(childState, child.getMeasuredState()
                            & (MEASURED_STATE_MASK>>MEASURED_HEIGHT_STATE_SHIFT));
                }
				//每次子控件比较，占用的最大宽度值
                final int margin =  lp.leftMargin + lp.rightMargin;
                final int measuredWidth = child.getMeasuredWidth() + margin;
                maxWidth = Math.max(maxWidth, measuredWidth);

                boolean matchWidthLocally = widthMode != MeasureSpec.EXACTLY &&
                        lp.width == LayoutParams.MATCH_PARENT;

                alternativeMaxWidth = Math.max(alternativeMaxWidth,
                        matchWidthLocally ? margin : measuredWidth);

                allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT;

                final int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + child.getMeasuredHeight() +
                        lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));
            }

            // Add in our padding
            mTotalLength += mPaddingTop + mPaddingBottom;
            // TODO: Should we recompute the heightSpec based on the new total length?
        } else {
            alternativeMaxWidth = Math.max(alternativeMaxWidth,
                                           weightedMaxWidth);


            // We have no limit, so make all weighted views as tall as the largest child.
            // Children will have already been measured once.
            if (useLargestChild && heightMode != MeasureSpec.EXACTLY) {
                for (int i = 0; i < count; i++) {
                    final View child = getVirtualChildAt(i);

                    if (child == null || child.getVisibility() == View.GONE) {
                        continue;
                    }

                    final LinearLayout.LayoutParams lp =
                            (LinearLayout.LayoutParams) child.getLayoutParams();

                    float childExtra = lp.weight;
                    if (childExtra > 0) {
                        child.measure(
                                MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(),
                                        MeasureSpec.EXACTLY),
                                MeasureSpec.makeMeasureSpec(largestChildHeight,
                                        MeasureSpec.EXACTLY));
                    }
                }
            }
        }

        if (!allFillParent && widthMode != MeasureSpec.EXACTLY) {
            maxWidth = alternativeMaxWidth;
        }
        //加上自身的padding值
        maxWidth += mPaddingLeft + mPaddingRight;
		//加上与默认值比较取较大值
        // Check against our minimum width
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
        //测量完毕，将宽高设置该LinearLayout的属性，使生效
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                heightSizeAndState);

        if (matchWidth) {
            forceUniformWidth(count, heightMeasureSpec);
        }
    }

```

**mTotalLength**--已经使用（后者分配）了的高度
**totalWeight**--子控件权重值总和

* 第一个for循环：
* - 忽略空元素、不占空间的子控件，即gone的元素
* - mTotalLength加上LinearLayout中该子元素前的分割线
* - * 该分割线有三种：start、middle、end，分别代表第一个子控件上边的分割线、子控件之间的分割线、最后一个子元素下面的分割线。
* - 累加权重到totalWeight中

这里分三种情况，

1. 若LinearLayout测量模式为Exactly,且子控件的height=0、weight>0：这时的子控件在本次循环中不测量，直接跳过，即相当于先分配0高度；

2. height=0、weight>0：这时的子控件会将height设置为warp_content，然后进行测量，即会先分配默认高度；
3. 其他（包括height!=0、weight>0的子控件），进行正常测量。

* - mTotalLength累加当前子控件占据的高度（包括子控件的margin值）

* mTotalLength加上最后一个子控件下面的分割线高度
* mTotalLength加上LinearLayout的padding值
* 和背景图片的高度比较得出较大值
* 综合考虑hieghtMeasureSpec与通过子控件计算的高度mTotalLenght确定LinearLayout的高度

* 最后一个for循环：根据累加的权重、剩余高度，对前面没有测量和带有权重的子控件测量一次，这次mTotalLenght置零重新累加
* - 根据子控件的权重计算新分配的高度，并测量子控件
* - 比较子控件的宽度，取较大值maxWidth
* 最终确定LinearLayout的宽高


##### 结论：
1. 当LinearLayout为Exactly模式时，
* height=0、weight>0的子控件只会在第二次for循环中测量一次。
* height=wrap_content、weight>0的子控件会在第一个fo循环中测量时设置为默认高度，第二次for循环会带上新分配的高度在测量一次。
* 而weight<=0的子控件只会在第一次for循环中测量一次。
2.当LinearLayout为At_most|unspecily时，
* height=0、weight>0的子控件会将height=0改为height=wrap_content，从而使用默认值在第一个for循环中测量，在第二个for循环中加上新分配的高度进行第二次测量。



### onLayout()布局方法

```
void layoutVertical(int left, int top, int right, int bottom) {
        final int paddingLeft = mPaddingLeft;
		//子控件可以放置的左、顶边界(不能小于这两个值)
        int childTop;
        int childLeft;
        
        // Where right end of child should go
        final int width = right - left;
        int childRight = width - mPaddingRight;
        
        // Space available for child
        int childSpace = width - paddingLeft - mPaddingRight;
        
        final int count = getVirtualChildCount();
		//主方向的对齐方式(竖直方向)
        final int majorGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
		//次方向的对齐方式(水平方向)
        final int minorGravity = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
		//根据竖直方向的对齐方式，计算子控件的上边界childTop的值
        switch (majorGravity) {
           case Gravity.BOTTOM:
               // mTotalLength contains the padding already
               childTop = mPaddingTop + bottom - top - mTotalLength;
               break;

               // mTotalLength contains the padding already
           case Gravity.CENTER_VERTICAL:
               childTop = mPaddingTop + (bottom - top - mTotalLength) / 2;
               break;

           case Gravity.TOP:
           default:
               childTop = mPaddingTop;
               break;
        }
		//循环遍历子控件对其布局
        for (int i = 0; i < count; i++) {
            final View child = getVirtualChildAt(i);
            if (child == null) {
                childTop += measureNullChild(i);
            } else if (child.getVisibility() != GONE) {
                final int childWidth = child.getMeasuredWidth();
                final int childHeight = child.getMeasuredHeight();
                
                final LinearLayout.LayoutParams lp =
                        (LinearLayout.LayoutParams) child.getLayoutParams();
                //获得子控件的对齐方式，默认值为-1，优先使用子控件的对齐方式
                //且子控件的对齐方式只在次方向上有效，否则使用默认值
                //这里次方向是水平，当child设置为top、bottom等时，此方向上会使用默认的left
                //当child设置为left、right等时，才会有效果
                int gravity = lp.gravity;
                if (gravity < 0) {
                    gravity = minorGravity;
                }
                final int layoutDirection = getLayoutDirection();
                final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
				//根据水平方向的对齐方式，计算子控件的左边界childLeft的值
                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = paddingLeft + ((childSpace - childWidth) / 2)
                                + lp.leftMargin - lp.rightMargin;
                        break;

                    case Gravity.RIGHT:
                        childLeft = childRight - childWidth - lp.rightMargin;
                        break;

                    case Gravity.LEFT:
                    default:
                        childLeft = paddingLeft + lp.leftMargin;
                        break;
                }
				//是否有分割线
                if (hasDividerBeforeChildAt(i)) {
                    childTop += mDividerHeight;
                }
				
                childTop += lp.topMargin;
                setChildFrame(child, childLeft, childTop + getLocationOffset(child),
                        childWidth, childHeight);
                childTop += childHeight + lp.bottomMargin + getNextLocationOffset(child);

                i += getChildrenSkipCount(child, i);
            }
        }
    }

```

在onLayout()方法中，只需确定子控件的左边界childLeft、上边界childTop两个值即可确定该子控件的位置。
* 首先根据在竖直方向上的对齐方式，确定第一个子控件的上边界childTop的值
* 在for中循环遍历子控件：
* - 根据子控件中设置的对齐方式、与LinearLayout中设置的对齐方式得出该子控件在LinearLayout中的对齐方式（注意：子控件只能设置水平方向上的对齐方式，否则默认为左对齐；子控件中设置的对齐方式优先级高于Linearlayout中设置的对齐方式）
* - 根据对齐方式确定子控件的左边界childLeft的值
* - 为下一个子控件的上边界做准备，childLeft累加分割线的高度、margin值。


##### onDraw()绘制方法

```
protected void onDraw(Canvas canvas) {
    	//LinearLayout是个容器，自身的内容只有分割线
        if (mDivider == null) {
            return;
        }
		//绘制分割线
        if (mOrientation == VERTICAL) {
            drawDividersVertical(canvas);
        } else {
            drawDividersHorizontal(canvas);
        }
    }
```


```
void drawDividersVertical(Canvas canvas) {
        final int count = getVirtualChildCount();
		//绘制子控件上面的分割线
        for (int i = 0; i < count; i++) {
            final View child = getVirtualChildAt(i);

            if (child != null && child.getVisibility() != GONE) {
                if (hasDividerBeforeChildAt(i)) {
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    final int top = child.getTop() - lp.topMargin - mDividerHeight;
                    drawHorizontalDivider(canvas, top);
                }
            }
        }
		//绘制最后一条分割线(在最后一个子控件的下面)
        if (hasDividerBeforeChildAt(count)) {
            final View child = getLastNonGoneChild();
            int bottom = 0;
            if (child == null) {
                bottom = getHeight() - getPaddingBottom() - mDividerHeight;
            } else {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                bottom = child.getBottom() + lp.bottomMargin;
            }
            drawHorizontalDivider(canvas, bottom);
        }
    }
```

* 根据布局方向绘制分割线


**总结**-这里只说竖直方向
* LinearLayout测量模式为Excetily时
* - height=0,weight>0的子控件只会测量一次，且是在第二次时测量
* - height!=0,weight>0的子控件会测量两次
* - weight=0的子控件只会测量一次，且是在第一次时测量
* LinearLayout测量模时不为精确模式时
* - height=0,weight>0的子控件会测量两次（height=0，中途会改成height=wrap_content）
* - height!=0,wieght>0的子控件会测量两次
* - weight=0的子控件只会测量一次，且是在第一次是测量