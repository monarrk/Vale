package net.verdagon.vale.hammer

import net.verdagon.vale.hammer.ExpressionHammer.translateDeferreds
import net.verdagon.vale.hinputs.Hinputs
import net.verdagon.vale.{vassert, vassertSome, vcurious, vfail, vwat, metal => m}
import net.verdagon.vale.metal.{ShareH => _, _}
import net.verdagon.vale.templar._
import net.verdagon.vale.templar.templata.{FunctionBanner2, FunctionHeader2, Prototype2}
import net.verdagon.vale.templar.types._

object CallHammer {

  def translateExternFunctionCall(
    hinputs: Hinputs,
    hamuts: HamutsBox,
    locals: LocalsBox,
    prototype2: Prototype2,
    argsExprs2: List[ReferenceExpression2]):
  (ExpressionH[ReferendH]) = {
    val (argsResultLines, argsDeferreds) =
      ExpressionHammer.translateExpressions(
        hinputs, hamuts, locals, argsExprs2);

    // Doublecheck the types
    val (paramTypes) =
      TypeHammer.translateReferences(hinputs, hamuts, prototype2.paramTypes);
    vassert(argsResultLines.map(_.resultType) == paramTypes)

    val (functionRefH) =
      FunctionHammer.translateFunctionRef(hinputs, hamuts, prototype2);

    val callResultNode = ExternCallH(functionRefH.prototype, argsResultLines)

      ExpressionHammer.translateDeferreds(
        hinputs, hamuts, locals, callResultNode, argsDeferreds)
  }

  def translateFunctionPointerCall(
      hinputs: Hinputs,
      hamuts: HamutsBox,
      locals: LocalsBox,
      function: Prototype2,
      args: List[Expression2],
      resultType2: Coord):
  ExpressionH[ReferendH] = {
    val returnType2 = function.returnType
    val paramTypes = function.paramTypes
    val (argLines, argsDeferreds) =
      ExpressionHammer.translateExpressions(
        hinputs, hamuts, locals, args);

    val prototypeH =
      FunctionHammer.translatePrototype(hinputs, hamuts, function)

    // Doublecheck the types
    val (paramTypesH) =
      TypeHammer.translateReferences(hinputs, hamuts, paramTypes)
    vassert(argLines.map(_.resultType) == paramTypesH)

    // Doublecheck return
    val (returnTypeH) = TypeHammer.translateReference(hinputs, hamuts, returnType2)
    val (resultTypeH) = TypeHammer.translateReference(hinputs, hamuts, resultType2);
    vassert(returnTypeH == resultTypeH)

    val callResultNode = CallH(prototypeH, argLines)

    ExpressionHammer.translateDeferreds(
      hinputs, hamuts, locals, callResultNode, argsDeferreds)
  }

  def translateConstructArray(
      hinputs: Hinputs, hamuts: HamutsBox,
      locals: LocalsBox,
      constructArray2: ConstructArray2):
  (ExpressionH[ReferendH]) = {
    val ConstructArray2(arrayType2, sizeExpr2, generatorExpr2) = constructArray2;

    val (sizeRegisterId, sizeDeferreds) =
      ExpressionHammer.translate(
        hinputs, hamuts, locals, sizeExpr2);

    val (generatorRegisterId, generatorDeferreds) =
      ExpressionHammer.translate(
        hinputs, hamuts, locals, generatorExpr2);

    val (arrayRefTypeH) =
      TypeHammer.translateReference(
        hinputs, hamuts, constructArray2.resultRegister.reference)

    val (arrayTypeH) =
      TypeHammer.translateUnknownSizeArray(hinputs, hamuts, arrayType2)
    vassert(arrayRefTypeH.expectUnknownSizeArrayReference().kind == arrayTypeH)

    val constructArrayCallNode =
        ConstructUnknownSizeArrayH(
          sizeRegisterId.expectIntAccess(),
          generatorRegisterId.expectInterfaceAccess(),
          arrayRefTypeH.expectUnknownSizeArrayReference())

    ExpressionHammer.translateDeferreds(
      hinputs, hamuts, locals, constructArrayCallNode, generatorDeferreds ++ sizeDeferreds)
  }

  def translateDestroyArraySequence(
      hinputs: Hinputs,
      hamuts: HamutsBox,
      locals: LocalsBox,
      das2: DestroyArraySequenceIntoFunction2):
  ExpressionH[ReferendH] = {
    val DestroyArraySequenceIntoFunction2(arrayExpr2, arraySequenceType, consumerExpr2) = das2;

    val KnownSizeArrayT2(size, rawArrayType2 @ RawArrayT2(memberType2, mutability)) = arraySequenceType

    val (arrayTypeH) =
      TypeHammer.translateKnownSizeArray(hinputs, hamuts, arraySequenceType)
    val (arrayRefTypeH) =
      TypeHammer.translateReference(hinputs, hamuts, arrayExpr2.resultRegister.reference)
    vassert(arrayRefTypeH.expectKnownSizeArrayReference().kind == arrayTypeH)

    val (arrayExprResultLine, arrayExprDeferreds) =
      ExpressionHammer.translate(
        hinputs, hamuts, locals, arrayExpr2);

    val (consumerCallableResultLine, consumerCallableDeferreds) =
      ExpressionHammer.translate(
        hinputs, hamuts, locals, consumerExpr2);

    val consumerInterfaceRef = consumerCallableResultLine.expectInterfaceAccess().resultType.kind;
    val consumerInterfaceDef = vassertSome(hamuts.interfaceDefs.values.find(_.getRef == consumerInterfaceRef))
    vassert(consumerInterfaceDef.methods.head.prototypeH.params.size == 2)
    vassert(consumerInterfaceDef.methods.head.prototypeH.params(0).kind == consumerInterfaceRef)
    vassert(consumerInterfaceDef.methods.head.prototypeH.params(1) == arrayTypeH.rawArray.elementType)

    val destroyArraySequenceCallNode =
        DestroyKnownSizeArrayIntoFunctionH(
          arrayExprResultLine.expectKnownSizeArrayAccess(),
          consumerCallableResultLine.expectInterfaceAccess())

    ExpressionHammer.translateDeferreds(
      hinputs, hamuts, locals, destroyArraySequenceCallNode, consumerCallableDeferreds ++ arrayExprDeferreds)
  }

  def translateDestroyUnknownSizeArray(
    hinputs: Hinputs,
    hamuts: HamutsBox,
    locals: LocalsBox,
    das2: DestroyUnknownSizeArray2):
  ExpressionH[ReferendH] = {
    val DestroyUnknownSizeArray2(arrayExpr2, unknownSizeArrayType2, consumerExpr2) = das2;

    val UnknownSizeArrayT2(RawArrayT2(memberType2, mutability)) = unknownSizeArrayType2

    val (arrayTypeH) =
      TypeHammer.translateUnknownSizeArray(hinputs, hamuts, unknownSizeArrayType2)
    val (arrayRefTypeH) =
      TypeHammer.translateReference(hinputs, hamuts, arrayExpr2.resultRegister.reference)
    vassert(arrayRefTypeH.expectUnknownSizeArrayReference().kind == arrayTypeH)

    val (arrayExprResultLine, arrayExprDeferreds) =
      ExpressionHammer.translate(
        hinputs, hamuts, locals, arrayExpr2);

    val (consumerCallableResultLine, consumerCallableDeferreds) =
      ExpressionHammer.translate(
        hinputs, hamuts, locals, consumerExpr2);

    val destroyArraySequenceCallNode =
        DestroyUnknownSizeArrayH(
          arrayExprResultLine.expectUnknownSizeArrayAccess(),
          consumerCallableResultLine.expectInterfaceAccess())

    ExpressionHammer.translateDeferreds(
      hinputs, hamuts, locals, destroyArraySequenceCallNode, consumerCallableDeferreds ++ arrayExprDeferreds)
  }

  def translateIf(
      hinputs: Hinputs, hamuts: HamutsBox,
    parentLocals: LocalsBox,
      if2: If2):
  ExpressionH[ReferendH] = {
    val If2(condition2, thenBlock2, elseBlock2) = if2

    val (conditionBlockH, List()) =
      ExpressionHammer.translate(hinputs, hamuts, parentLocals, condition2);
    vassert(conditionBlockH.resultType == ReferenceH(m.ShareH, InlineH,BoolH()))

    val thenLocals = LocalsBox(parentLocals.snapshot)
    val (thenBlockH, List()) =
      ExpressionHammer.translate(hinputs, hamuts, thenLocals, thenBlock2);
    val thenResultCoord = thenBlockH.resultType

    val elseLocals = LocalsBox(parentLocals.snapshot)
    val (elseBlockH, List()) =
      ExpressionHammer.translate(hinputs, hamuts, elseLocals, elseBlock2);
    val elseResultCoord = elseBlockH.resultType

    val commonSupertypeH =
      TypeHammer.translateReference(hinputs, hamuts, if2.resultRegister.reference)

    val ifCallNode = IfH(conditionBlockH.expectBoolAccess(), thenBlockH, elseBlockH, commonSupertypeH)


    val thenContinues = thenResultCoord.kind != NeverH()
    val elseContinues = elseResultCoord.kind != NeverH()

    val localsFromBranchToUseForUnstackifyingParentLocals =
      if (thenContinues == elseContinues) { // Both continue, or both don't
        val parentLocalsAfterThen = thenLocals.locals.keySet -- thenLocals.unstackifiedVars
        val parentLocalsAfterElse = elseLocals.locals.keySet -- elseLocals.unstackifiedVars
        // The same outside-if variables should still exist no matter which branch we went down.
        vassert(parentLocalsAfterThen == parentLocalsAfterElse)
        // Since theyre the same, just arbitrarily use the then.
        thenLocals
      } else {
        // One of them continues and the other does not.
        if (thenContinues) {
          // Throw away any information from the else. But do consider those from the then.
          thenLocals
        } else if (elseContinues) {
          elseLocals
        } else vfail()
      }

    val parentLocalsToUnstackify =
      // All the parent locals...
      parentLocals.locals.keySet
        // ...minus the ones that were unstackified before...
        .diff(parentLocals.unstackifiedVars)
        // ...which were unstackified by the branch.
        .intersect(localsFromBranchToUseForUnstackifyingParentLocals.unstackifiedVars)
    parentLocalsToUnstackify.foreach(parentLocals.markUnstackified)

    ifCallNode
  }

  def translateWhile(
      hinputs: Hinputs, hamuts: HamutsBox,
      locals: LocalsBox,
      while2: While2):
  WhileH = {

    val While2(bodyExpr2) = while2

    val (exprWithoutDeferreds, deferreds) =
      ExpressionHammer.translate(hinputs, hamuts, locals, bodyExpr2);
    val expr =
      translateDeferreds(hinputs, hamuts, locals, exprWithoutDeferreds, deferreds)

    val boolExpr = expr.expectBoolAccess()

    val whileCallNode = WhileH(boolExpr)
    whileCallNode
  }

  def translateInterfaceFunctionCall(
      hinputs: Hinputs,
      hamuts: HamutsBox,
      locals: LocalsBox,
      superFunctionHeader: FunctionHeader2,
      resultType2: Coord,
      argsExprs2: List[Expression2]):
  ExpressionH[ReferendH] = {
    val (argLines, argsDeferreds) =
      ExpressionHammer.translateExpressions(
        hinputs, hamuts, locals, argsExprs2);

    val virtualParamIndex = superFunctionHeader.getVirtualIndex.get
    val Coord(_, interfaceRef2 @ InterfaceRef2(_)) =
      superFunctionHeader.paramTypes(virtualParamIndex)
    val (interfaceRefH) =
      StructHammer.translateInterfaceRef(hinputs, hamuts, interfaceRef2)
    val edge = hinputs.edgeBlueprintsByInterface(interfaceRef2)
    vassert(edge.interface == interfaceRef2)
    val indexInEdge = edge.superFamilyRootBanners.indexOf(superFunctionHeader.toBanner)
    vassert(indexInEdge >= 0)

    val (prototypeH) = FunctionHammer.translatePrototype(hinputs, hamuts, superFunctionHeader.toPrototype)

    val callNode =
        InterfaceCallH(
          argLines,
          virtualParamIndex,
          interfaceRefH,
          indexInEdge,
          prototypeH)

    ExpressionHammer.translateDeferreds(
      hinputs, hamuts, locals, callNode, argsDeferreds)

    //
//    val (callResultLine) =
//      superFamilyRootBanner.params.zipWithIndex.collectFirst({
//        case (Parameter2(_, Some(_), Coord(_, interfaceRef2 : InterfaceRef2)), paramIndex) => {
//          val (interfaceRefH) =
//            StructHammer.translateInterfaceRef(hinputs, hamuts, interfaceRef2)
//
//          val (functionNodeLine) =
//            translateInterfaceFunctionCallWithInterface(
//              hinputs,
//              hamuts,
//              nodesByLine,
//              superFamilyRootBanner,
//              paramIndex,
//              interfaceRefH,
//              functionTypeH,
//              argLines)
//          (functionNodeLine)
//        }
//        case (Parameter2(_, Some(_), Coord(_, structRef2@ StructRef2(_))), _) => {
//          val (functionRegister) =
//            translateInterfaceFunctionLookupWithStruct(
//              hinputs,
//              hamuts,
//              nodesByLine,
//              structRef2,
//              superFamilyRootBanner)
//          val callResultNode =
//            addNode(
//              nodesByLine,
//              CallH(
//                nodesByLine.nextId(),
//                functionRegister,
//                argLines));
//
//          val returnType2 = functionRegister.expectedType.expectFunctionReference().innerType.returnType
//          val access =
//            if (returnType2 == ReferenceH(m.Share, VoidH())) {
//              None
//            } else {
//              Some(NodeH(callResultNode.registerId, returnType2))
//            }
//          (access)
//        }
//      }).get
//
//
//      ExpressionHammer.translateDeferreds(
//        hinputs, hamuts, locals, argsDeferreds)
//    (callResultLine)
  }
//
//  private def translateInterfaceFunctionLookupWithStruct(
//      hinputs: Hinputs,
//      hamuts: HamutsBox,
//      nodesByLine: NodesBox,
//      structRef2: StructRef2,
//      superFamilyRootBanner: FunctionBanner2):
//  (Vector[NodeH], NodeH[FunctionTH]) = {
//    val prototype2 =
//      getPrototypeForStructInterfaceCall(hinputs, structRef2, superFamilyRootBanner)
//
//    val (functionRefH) =
//      FunctionHammer.translateFunctionRef(hinputs, hamuts, prototype2);
//    val functionNode =
//      addNode(
//        nodesByLine,
//        LoadFunctionH(nodesByLine.nextId(), functionRefH));
//    val access = NodeH(functionNode.registerId, ReferenceH(m.Raw, functionRefH.functionType))
//    (access)
//  }
//
//  private def translateInterfaceFunctionCallWithInterface(
//      hinputs: Hinputs,
//      hamuts: HamutsBox,
//      nodesByLine: NodesBox,
//      superFamilyRootBanner: FunctionBanner2,
//      firstVirtualParamIndex: Int,
//      firstVirtualParamInterface: InterfaceRefH,
//      functionTypeH: FunctionTH,
//      argLines: List[NodeH[ReferendH]]):
//  (Vector[NodeH], Option[NodeH[ReferendH]]) = {
//    val interfaceId = firstVirtualParamInterface.interfaceId
//
//    val edgeBlueprint =
//      hinputs.edgeBlueprintsByInterfaceId(interfaceId)
//    val indexInEdge =
//      edgeBlueprint.superFamilyRootBanners.indexOf(superFamilyRootBanner)
//    if (indexInEdge < 0) {
//      vfail("Can't find:\n" + superFamilyRootBanner + "\nin:\n" + edgeBlueprint.interface)
//    }
//
//    val methodNode =
//      addNode(
//        nodesByLine,
//        InterfaceCallH(
//          nodesByLine.nextId(),
//          argLines,
//          firstVirtualParamIndex,
//          firstVirtualParamInterface,
//          interfaceId,
//          indexInEdge,
//          functionTypeH));
//
//    val access =
//      if (functionTypeH.returnType == ReferenceH(m.Share, VoidH())) {
//        None
//      } else {
//        Some(NodeH(methodNode.registerId, functionTypeH.returnType))
//      }
//    (access)
//  }

//  private def getPrototypeForStructInterfaceCall(
//      hinputs: Hinputs,
//      structRef2: StructRef2,
//      superFamilyRootBanner: FunctionBanner2):
//  Prototype2 = {
//
//    val structDef2 = hinputs.lookupStruct(structRef2)
//    val ancestorInterfaces2 =
//      hinputs.impls.filter(impl => impl.struct == structDef2.getRef).map(_.interface)
//    val edgeBlueprints = ancestorInterfaces2.map(hinputs.edgeBlueprintsByInterface)
//    val matchingEdgeBlueprint =
//      edgeBlueprints.find(_.superFamilyRootBanners.contains(superFamilyRootBanner)).get;
//
//    val indexInEdgeBlueprint = matchingEdgeBlueprint.superFamilyRootBanners.indexOf(superFamilyRootBanner);
//    vassert(indexInEdgeBlueprint >= 0);
//
//    val edge =
//      hinputs.edges.find(
//        edge => edge.interface == matchingEdgeBlueprint.interface && edge.struct == structRef2).get;
//    val methodPrototype2 = edge.methods(indexInEdgeBlueprint)
//    methodPrototype2
//  }

}
