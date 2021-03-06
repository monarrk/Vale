package net.verdagon.vale

object ArrayUtils {
  val code =
    """
      |fn toArray<M, N, E, SM>(seq &[<SM> N * E]) rules(M Mutability) {
      |  Array<M, E>(N, &IFunction1<SM, int, E>((i){ seq[i]}))
      |}
      |
      |fn has<E, F>(arr &Array<_, E>, elem E, equator F) bool {
      |  i = 0;
      |  while (i < arr.len()) {
      |    if ((equator)(arr[i], elem)) {
      |      ret true;
      |    }
      |    mut i = i + 1;
      |  }
      |  = false;
      |}
      |
      |fn has<E>(arr &Array<_, E>, elem E) bool {
      |  has(arr, elem, ==)
      |}
      |
      |fn has<E, F>(seq &[<_> _ * E], elem E, equator F) bool {
      |  i = 0;
      |  while (i < seq.len()) {
      |    if ((equator)(seq[i], elem)) {
      |      ret true;
      |    }
      |    mut i = i + 1;
      |  }
      |  = false;
      |}
      |
      |fn has<E>(seq &[<_> _ * E], elem E) bool {
      |  has(seq, elem, ==)
      |}
      |
      |fn Arr<M, F>(n int, generator &F) Array<M, T>
      |rules(M Mutability, T Ref, Prot("__call", (&F, int), T))
      |{
      |  Array<M>(n, &IFunction1<mut, int, T>(generator))
      |}
      |
      |fn each<M, T, F>(arr &Array<M, T>, func F) void {
      |  i! = 0;
      |  l = len(&arr);
      |  while (i < l) {
      |    func(arr[i]);
      |    mut i = i + 1;
      |  }
      |}
      |
      |fn indices<>(arr &Array<_, _>) Array<imm, int> {
      |  Arr<imm>(len(arr), {_})
      |}
      |
      |fn eachI<F>(arr &Array<_, _>, func F) void {
      |  i! = 0;
      |  l = len(&arr);
      |  while (i < l) {
      |    func(i, arr[i]);
      |    mut i = i + 1;
      |  }
      |}
      |
      |""".stripMargin
}
